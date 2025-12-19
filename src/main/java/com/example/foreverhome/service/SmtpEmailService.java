package com.example.foreverhome.service;

import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.logging.UserJourneyLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * SMTP-based email service for local development with Mailpit.
 * Sends real emails via SMTP that can be viewed in Mailpit UI at http://localhost:8025
 */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String baseUrl;
    private final MetricsService metricsService;
    private final UserJourneyLogger journeyLogger;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.email.from:noreply@foreverhome.local}") String fromEmail,
            @Value("${app.base-url:http://localhost:5173}") String baseUrl,
            MetricsService metricsService,
            UserJourneyLogger journeyLogger) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.baseUrl = baseUrl;
        this.metricsService = metricsService;
        this.journeyLogger = journeyLogger;
        logger.info("SmtpEmailService initialized - emails will be sent via SMTP (Mailpit UI: http://localhost:8025)");
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

        sendEmail(to, subject, body, "verification");
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

        sendEmail(to, subject, body, "password_reset");
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

        sendEmail(to, subject, body, "admin_password_reset");
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String body) {
        sendEmail(to, subject, body, "notification");
    }

    @Override
    public void sendWelcomeEmail(String to, String name, UserRole role) {
        String subject = "Welcome to Forever Home!";
        String body = switch (role) {
            case FOSTER -> """
                Hi %s,

                Thank you for opening your heart to help a pet find their forever home.

                Every animal deserves a second chance, and by fostering or rehoming a pet through
                Forever Home, you're making that possible.

                Here's how to get started:
                - Complete your profile with your contact details
                - Register your pet with photos and a heartfelt description
                - Connect with rescue organizations in your area
                - Review applications from loving adopters

                With gratitude,
                The Forever Home Team
                """.formatted(name);
            case ADOPTER -> """
                Hi %s,

                Welcome to Forever Home! Your journey to finding a furry friend starts here.

                We connect pet lovers like you with rescue organizations to help pets find
                their forever families. Your perfect companion is waiting to meet you.

                Here's what you can do next:
                - Complete your profile to get started
                - Browse available pets looking for homes
                - Save your favorite pets to revisit later
                - Submit adoption applications when you find the one

                Happy pet matching!
                The Forever Home Team
                """.formatted(name);
            case VET -> """
                Hi %s,

                Thank you for joining Forever Home as a veterinary professional.

                Your expertise is vital to ensuring every pet is healthy and ready for their
                new family. We appreciate your dedication to animal welfare.

                Here's how to get started:
                - Complete your clinic profile with license details
                - Wait for verification from a rescue organization
                - Review and sign off on pet health records
                - Confirm vaccination and neutering status

                Thank you for your service,
                The Forever Home Team
                """.formatted(name);
            case RESCUE_ORG -> """
                Hi %s,

                Welcome to the Forever Home rescue network!

                We're excited to partner with you in our mission to connect pets with loving
                families. Together, we can make a real difference in the lives of animals in need.

                Here's how to get started:
                - Complete your organization profile with contact details
                - Wait for admin approval of your organization
                - Verify veterinarians to work with your rescue
                - Start managing pet listings and adoption applications

                Thank you for joining us,
                The Forever Home Team
                """.formatted(name);
            case ADMIN -> """
                Hi %s,

                You've been granted administrator access to Forever Home.

                You now have the tools to help manage our platform and support our community
                of fosters, adopters, vets, and rescue organizations.

                Your admin capabilities include:
                - Approve rescue organization registrations
                - Manage user accounts and access
                - Monitor platform activity and metrics
                - Handle support escalations

                Welcome aboard,
                The Forever Home Team
                """.formatted(name);
        };

        sendEmail(to, subject, body, "welcome");
    }

    private void sendEmail(String to, String subject, String body, String emailType) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            metricsService.recordEmailSent(emailType);
            journeyLogger.logEmail(UserJourneyLogger.ACTION_EMAIL_SENT, emailType, to, true, "Subject: " + subject);
            logger.info("Email sent to {}: {} (view at http://localhost:8025)", to, subject);
        } catch (Exception e) {
            metricsService.recordEmailFailed(emailType);
            journeyLogger.logEmail(UserJourneyLogger.ACTION_EMAIL_FAILED, emailType, to, false, e.getMessage());
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
