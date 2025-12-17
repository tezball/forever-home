package com.example.foreverhome.service;

import com.example.foreverhome.logging.UserJourneyLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "ses")
public class SesEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SesEmailService.class);

    private final SesClient sesClient;
    private final String fromEmail;
    private final MetricsService metricsService;
    private final UserJourneyLogger journeyLogger;
    private final EmailTemplateService templateService;
    private final String configurationSetName;

    public SesEmailService(
            SesClient sesClient,
            @Value("${app.email.from:noreply@foreverhome.local}") String fromEmail,
            @Value("${aws.ses.configuration-set:}") String configurationSetName,
            MetricsService metricsService,
            UserJourneyLogger journeyLogger,
            EmailTemplateService templateService) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
        this.configurationSetName = configurationSetName;
        this.metricsService = metricsService;
        this.journeyLogger = journeyLogger;
        this.templateService = templateService;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        // Extract name from email for personalization (before @)
        String recipientName = extractNameFromEmail(to);
        EmailTemplateService.EmailContent content = templateService.generateVerificationEmail(recipientName, token);
        sendEmail(to, content.subject(), content.htmlBody(), content.textBody());
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String recipientName = extractNameFromEmail(to);
        EmailTemplateService.EmailContent content = templateService.generatePasswordResetEmail(recipientName, token);
        sendEmail(to, content.subject(), content.htmlBody(), content.textBody());
    }

    @Override
    public void sendPasswordResetByAdmin(String to, String temporaryPassword) {
        String recipientName = extractNameFromEmail(to);
        EmailTemplateService.EmailContent content = templateService.generateAdminPasswordResetEmail(recipientName, temporaryPassword);
        sendEmail(to, content.subject(), content.htmlBody(), content.textBody());
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String body) {
        String recipientName = extractNameFromEmail(to);
        EmailTemplateService.EmailContent content = templateService.generateNotificationEmail(recipientName, subject, body);
        sendEmail(to, content.subject(), content.htmlBody(), content.textBody());
    }

    @Override
    public void sendWelcomeEmail(String to, String name) {
        EmailTemplateService.EmailContent content = templateService.generateWelcomeEmail(name);
        sendEmail(to, content.subject(), content.htmlBody(), content.textBody());
    }

    private void sendEmail(String to, String subject, String htmlBody, String textBody) {
        String emailType = getEmailType(subject);
        try {
            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .text(Content.builder().data(textBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .source(fromEmail);

            // Add configuration set for CloudWatch tracking if configured
            if (configurationSetName != null && !configurationSetName.isBlank()) {
                requestBuilder.configurationSetName(configurationSetName);
            }

            sesClient.sendEmail(requestBuilder.build());
            metricsService.recordEmailSent(emailType);
            journeyLogger.logEmail(UserJourneyLogger.ACTION_EMAIL_SENT, emailType, to, true, "Subject: " + subject);
            logger.info("Email sent to {}: {}", to, subject);
        } catch (SesException e) {
            metricsService.recordEmailFailed(emailType);
            journeyLogger.logEmail(UserJourneyLogger.ACTION_EMAIL_FAILED, emailType, to, false, e.getMessage());
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            // Don't throw - email failure shouldn't break the flow
        }
    }

    private String getEmailType(String subject) {
        if (subject.contains("Verify")) return "verification";
        if (subject.contains("Reset") || subject.contains("reset")) return "password_reset";
        if (subject.contains("Welcome")) return "welcome";
        if (subject.contains("notification")) return "notification";
        return "other";
    }

    /**
     * Extract a display name from email address.
     * Converts "john.doe@example.com" to "John Doe".
     */
    private String extractNameFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return "there";
        }
        String localPart = email.split("@")[0];
        // Replace common separators with spaces and capitalize each word
        String[] parts = localPart.split("[._-]");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (!name.isEmpty()) {
                    name.append(" ");
                }
                name.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    name.append(part.substring(1).toLowerCase());
                }
            }
        }
        return name.toString().isEmpty() ? "there" : name.toString();
    }
}
