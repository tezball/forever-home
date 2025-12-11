package com.example.foreverhome.service;

import com.example.foreverhome.logging.UserJourneyLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.verification.use-console", havingValue = "true")
public class ConsoleEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleEmailService.class);

    private final String baseUrl;
    private final MetricsService metricsService;
    private final UserJourneyLogger journeyLogger;

    public ConsoleEmailService(
            @Value("${app.base-url:http://localhost:5173}") String baseUrl,
            MetricsService metricsService,
            UserJourneyLogger journeyLogger) {
        this.baseUrl = baseUrl;
        this.metricsService = metricsService;
        this.journeyLogger = journeyLogger;
        logger.info("ConsoleEmailService initialized - emails will be logged to console");
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        String verificationLink = baseUrl + "/verify-email?token=" + token;
        metricsService.recordEmailSent("verification");
        journeyLogger.logEmail(UserJourneyLogger.ACTION_EMAIL_SENT, "verification", to, true, "Verification email sent");
        logger.info("""

            ╔══════════════════════════════════════════════════════════════════╗
            ║                    EMAIL VERIFICATION                            ║
            ╠══════════════════════════════════════════════════════════════════╣
            ║  To: {}
            ║
            ║  Verification Link:
            ║  {}
            ║
            ║  Or call API directly:
            ║  curl -X POST http://localhost:8080/api/auth/verify-email/{}
            ╚══════════════════════════════════════════════════════════════════╝
            """, to, verificationLink, token);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        metricsService.recordEmailSent("password_reset");
        journeyLogger.logEmail(UserJourneyLogger.ACTION_EMAIL_SENT, "password_reset", to, true, "Password reset email sent");
        logger.info("""

            ╔══════════════════════════════════════════════════════════════════╗
            ║                    PASSWORD RESET                                ║
            ╠══════════════════════════════════════════════════════════════════╣
            ║  To: {}
            ║
            ║  Reset Link:
            ║  {}
            ║
            ║  Token: {}
            ╚══════════════════════════════════════════════════════════════════╝
            """, to, resetLink, token);
    }

    @Override
    public void sendPasswordResetByAdmin(String to, String temporaryPassword) {
        metricsService.recordEmailSent("admin_password_reset");
        journeyLogger.logEmail(UserJourneyLogger.ACTION_EMAIL_SENT, "admin_password_reset", to, true, "Admin password reset email sent");
        logger.info("""

            ╔══════════════════════════════════════════════════════════════════╗
            ║                  ADMIN PASSWORD RESET                            ║
            ╠══════════════════════════════════════════════════════════════════╣
            ║  To: {}
            ║
            ║  Your password has been reset by an administrator.
            ║  Temporary Password: {}
            ║
            ║  Please log in and change your password immediately.
            ╚══════════════════════════════════════════════════════════════════╝
            """, to, temporaryPassword);
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String body) {
        metricsService.recordEmailSent("notification");
        journeyLogger.logEmail(UserJourneyLogger.ACTION_EMAIL_SENT, "notification", to, true, "Subject: " + subject);
        logger.info("""

            ╔══════════════════════════════════════════════════════════════════╗
            ║                    NOTIFICATION EMAIL                            ║
            ╠══════════════════════════════════════════════════════════════════╣
            ║  To: {}
            ║  Subject: {}
            ║
            ║  Body:
            ║  {}
            ╚══════════════════════════════════════════════════════════════════╝
            """, to, subject, body);
    }
}
