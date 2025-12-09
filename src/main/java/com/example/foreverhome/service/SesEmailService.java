package com.example.foreverhome.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@ConditionalOnProperty(name = "app.email.verification.use-console", havingValue = "false", matchIfMissing = true)
public class SesEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SesEmailService.class);

    private final SesClient sesClient;
    private final String fromEmail;
    private final String baseUrl;

    public SesEmailService(
            SesClient sesClient,
            @Value("${app.email.from:noreply@foreverhome.local}") String fromEmail,
            @Value("${app.base-url:http://localhost:5173}") String baseUrl) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify your Forever Home account";
        String verificationLink = baseUrl + "/verify-email?token=" + token;
        String body = """
            Welcome to Forever Home!

            Please click the link below to verify your email address:
            %s

            This link will expire in 24 hours.

            If you didn't create an account, please ignore this email.
            """.formatted(verificationLink);

        sendEmail(to, subject, body);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Reset your Forever Home password";
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String body = """
            You requested a password reset for your Forever Home account.

            Click the link below to reset your password:
            %s

            This link will expire in 1 hour.

            If you didn't request this, please ignore this email.
            """.formatted(resetLink);

        sendEmail(to, subject, body);
    }

    @Override
    public void sendPasswordResetByAdmin(String to, String temporaryPassword) {
        String subject = "Your Forever Home password has been reset";
        String body = """
            An administrator has reset your Forever Home account password.

            Your temporary password is: %s

            Please log in and change your password immediately for security.

            If you didn't expect this reset, please contact support.
            """.formatted(temporaryPassword);

        sendEmail(to, subject, body);
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String body) {
        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder()
                                    .text(Content.builder().data(body).build())
                                    .build())
                            .build())
                    .source(fromEmail)
                    .build();

            sesClient.sendEmail(request);
            logger.info("Email sent to {}: {}", to, subject);
        } catch (SesException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            // Don't throw - email failure shouldn't break the flow
        }
    }
}
