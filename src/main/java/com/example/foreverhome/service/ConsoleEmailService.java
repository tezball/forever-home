package com.example.foreverhome.service;

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

    public ConsoleEmailService(@Value("${app.base-url:http://localhost:5173}") String baseUrl) {
        this.baseUrl = baseUrl;
        logger.info("ConsoleEmailService initialized - emails will be logged to console");
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        String verificationLink = baseUrl + "/verify-email?token=" + token;
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
    public void sendNotificationEmail(String to, String subject, String body) {
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
