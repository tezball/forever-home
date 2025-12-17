package com.example.foreverhome.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for generating HTML and plain text email content.
 * Uses Forever Home brand colors from the UI style guide.
 */
@Service
public class EmailTemplateService {

    private final String baseUrl;

    public EmailTemplateService(@Value("${app.base-url:http://localhost:5173}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Email content record containing both HTML and plain text versions.
     */
    public record EmailContent(String subject, String htmlBody, String textBody) {}

    /**
     * Generate verification email content.
     */
    public EmailContent generateVerificationEmail(String recipientName, String token) {
        String verificationLink = baseUrl + "/verify-email?token=" + token;
        String subject = "Verify your Forever Home account";

        String htmlBody = wrapInTemplate(subject, """
            <h1 style="color: #1D3A2F; font-family: 'Georgia', serif; font-size: 24px; margin: 0 0 16px 0;">
                Welcome to Forever Home!
            </h1>
            <p style="color: #1A1A1A; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                Hi %s,
            </p>
            <p style="color: #1A1A1A; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                Thank you for joining Forever Home! Please verify your email address to complete your registration
                and start connecting with rescue organizations.
            </p>
            <div style="text-align: center; margin: 32px 0;">
                <a href="%s"
                   style="background-color: #2D5A47; color: #FFFFFF; padding: 14px 32px; text-decoration: none;
                          border-radius: 6px; font-weight: 600; font-size: 16px; display: inline-block;">
                    Verify Email Address
                </a>
            </div>
            <p style="color: #5C5C5C; font-size: 14px; line-height: 1.6; margin: 24px 0 0 0;">
                Or copy and paste this link into your browser:
            </p>
            <p style="color: #2D5A47; font-size: 14px; word-break: break-all; margin: 8px 0 24px 0;">
                %s
            </p>
            <p style="color: #5C5C5C; font-size: 14px; line-height: 1.6; margin: 0;">
                This link will expire in 24 hours. If you didn't create an account, please ignore this email.
            </p>
            """.formatted(escapeHtml(recipientName), verificationLink, verificationLink));

        String textBody = """
            Welcome to Forever Home!

            Hi %s,

            Thank you for joining Forever Home! Please verify your email address to complete your registration.

            Click this link to verify your email:
            %s

            This link will expire in 24 hours.

            If you didn't create an account, please ignore this email.

            ---
            Forever Home - Finding forever families for pets
            """.formatted(recipientName, verificationLink);

        return new EmailContent(subject, htmlBody, textBody);
    }

    /**
     * Generate password reset email content.
     */
    public EmailContent generatePasswordResetEmail(String recipientName, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String subject = "Reset your Forever Home password";

        String htmlBody = wrapInTemplate(subject, """
            <h1 style="color: #1D3A2F; font-family: 'Georgia', serif; font-size: 24px; margin: 0 0 16px 0;">
                Password Reset Request
            </h1>
            <p style="color: #1A1A1A; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                Hi %s,
            </p>
            <p style="color: #1A1A1A; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                We received a request to reset your Forever Home password. Click the button below to create a new password.
            </p>
            <div style="text-align: center; margin: 32px 0;">
                <a href="%s"
                   style="background-color: #2D5A47; color: #FFFFFF; padding: 14px 32px; text-decoration: none;
                          border-radius: 6px; font-weight: 600; font-size: 16px; display: inline-block;">
                    Reset Password
                </a>
            </div>
            <p style="color: #5C5C5C; font-size: 14px; line-height: 1.6; margin: 24px 0 0 0;">
                Or copy and paste this link into your browser:
            </p>
            <p style="color: #2D5A47; font-size: 14px; word-break: break-all; margin: 8px 0 24px 0;">
                %s
            </p>
            <p style="color: #C45A5A; font-size: 14px; line-height: 1.6; margin: 0 0 16px 0;">
                <strong>This link will expire in 1 hour.</strong>
            </p>
            <p style="color: #5C5C5C; font-size: 14px; line-height: 1.6; margin: 0;">
                If you didn't request a password reset, please ignore this email. Your password will remain unchanged.
            </p>
            """.formatted(escapeHtml(recipientName), resetLink, resetLink));

        String textBody = """
            Password Reset Request

            Hi %s,

            We received a request to reset your Forever Home password.

            Click this link to reset your password:
            %s

            This link will expire in 1 hour.

            If you didn't request a password reset, please ignore this email.

            ---
            Forever Home - Finding forever families for pets
            """.formatted(recipientName, resetLink);

        return new EmailContent(subject, htmlBody, textBody);
    }

    /**
     * Generate admin password reset email content.
     */
    public EmailContent generateAdminPasswordResetEmail(String recipientName, String temporaryPassword) {
        String subject = "Your Forever Home password has been reset";

        String htmlBody = wrapInTemplate(subject, """
            <h1 style="color: #1D3A2F; font-family: 'Georgia', serif; font-size: 24px; margin: 0 0 16px 0;">
                Password Reset by Administrator
            </h1>
            <p style="color: #1A1A1A; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                Hi %s,
            </p>
            <p style="color: #1A1A1A; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                An administrator has reset your Forever Home account password.
            </p>
            <div style="background-color: #F5F0E8; border: 1px solid #E8E4DC; border-radius: 6px; padding: 20px; margin: 24px 0; text-align: center;">
                <p style="color: #5C5C5C; font-size: 14px; margin: 0 0 8px 0;">Your temporary password:</p>
                <p style="color: #1D3A2F; font-size: 20px; font-weight: 600; font-family: monospace; margin: 0;">
                    %s
                </p>
            </div>
            <p style="color: #C45A5A; font-size: 14px; line-height: 1.6; margin: 0 0 16px 0;">
                <strong>Please log in and change your password immediately for security.</strong>
            </p>
            <div style="text-align: center; margin: 32px 0;">
                <a href="%s/login"
                   style="background-color: #2D5A47; color: #FFFFFF; padding: 14px 32px; text-decoration: none;
                          border-radius: 6px; font-weight: 600; font-size: 16px; display: inline-block;">
                    Log In Now
                </a>
            </div>
            <p style="color: #5C5C5C; font-size: 14px; line-height: 1.6; margin: 0;">
                If you didn't expect this reset, please contact support immediately.
            </p>
            """.formatted(escapeHtml(recipientName), escapeHtml(temporaryPassword), baseUrl));

        String textBody = """
            Password Reset by Administrator

            Hi %s,

            An administrator has reset your Forever Home account password.

            Your temporary password is: %s

            Please log in and change your password immediately for security.

            Log in at: %s/login

            If you didn't expect this reset, please contact support immediately.

            ---
            Forever Home - Finding forever families for pets
            """.formatted(recipientName, temporaryPassword, baseUrl);

        return new EmailContent(subject, htmlBody, textBody);
    }

    /**
     * Generate welcome email content.
     */
    public EmailContent generateWelcomeEmail(String recipientName) {
        String subject = "Welcome to Forever Home!";

        String htmlBody = wrapInTemplate(subject, """
            <h1 style="color: #1D3A2F; font-family: 'Georgia', serif; font-size: 24px; margin: 0 0 16px 0;">
                Welcome to Forever Home!
            </h1>
            <p style="color: #1A1A1A; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                Hi %s,
            </p>
            <p style="color: #1A1A1A; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                We're thrilled to have you join our community! Forever Home connects pet lovers with rescue
                organizations to help pets find their forever families.
            </p>
            <h2 style="color: #2D5A47; font-size: 18px; margin: 32px 0 16px 0;">
                Here's what you can do next:
            </h2>
            <ul style="color: #1A1A1A; font-size: 16px; line-height: 1.8; margin: 0 0 24px 0; padding-left: 24px;">
                <li>Complete your profile to get started</li>
                <li>Browse available pets looking for homes</li>
                <li>Connect with rescue organizations in your area</li>
                <li>Save your favorite pets to revisit later</li>
            </ul>
            <div style="text-align: center; margin: 32px 0;">
                <a href="%s"
                   style="background-color: #2D5A47; color: #FFFFFF; padding: 14px 32px; text-decoration: none;
                          border-radius: 6px; font-weight: 600; font-size: 16px; display: inline-block;">
                    Explore Forever Home
                </a>
            </div>
            <p style="color: #5C5C5C; font-size: 14px; line-height: 1.6; margin: 24px 0 0 0;">
                If you have any questions, feel free to reach out to our support team. We're here to help!
            </p>
            <p style="color: #2D5A47; font-size: 16px; font-style: italic; margin: 24px 0 0 0;">
                Happy pet matching!<br>
                The Forever Home Team
            </p>
            """.formatted(escapeHtml(recipientName), baseUrl));

        String textBody = """
            Welcome to Forever Home!

            Hi %s,

            We're thrilled to have you join our community! Forever Home connects pet lovers with rescue
            organizations to help pets find their forever families.

            Here's what you can do next:
            - Complete your profile to get started
            - Browse available pets looking for homes
            - Connect with rescue organizations in your area
            - Save your favorite pets to revisit later

            Visit: %s

            If you have any questions, feel free to reach out to our support team.

            Happy pet matching!
            The Forever Home Team

            ---
            Forever Home - Finding forever families for pets
            """.formatted(recipientName, baseUrl);

        return new EmailContent(subject, htmlBody, textBody);
    }

    /**
     * Generate generic notification email content.
     */
    public EmailContent generateNotificationEmail(String recipientName, String notificationSubject, String notificationBody) {
        String htmlBody = wrapInTemplate(notificationSubject, """
            <p style="color: #1A1A1A; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                Hi %s,
            </p>
            <div style="color: #1A1A1A; font-size: 16px; line-height: 1.6;">
                %s
            </div>
            <div style="text-align: center; margin: 32px 0;">
                <a href="%s"
                   style="background-color: #2D5A47; color: #FFFFFF; padding: 14px 32px; text-decoration: none;
                          border-radius: 6px; font-weight: 600; font-size: 16px; display: inline-block;">
                    View on Forever Home
                </a>
            </div>
            """.formatted(escapeHtml(recipientName), escapeHtml(notificationBody), baseUrl));

        String textBody = """
            Hi %s,

            %s

            Visit Forever Home: %s

            ---
            Forever Home - Finding forever families for pets
            """.formatted(recipientName, notificationBody, baseUrl);

        return new EmailContent(notificationSubject, htmlBody, textBody);
    }

    /**
     * Wrap content in the base HTML email template with brand styling.
     */
    private String wrapInTemplate(String title, String content) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="X-UA-Compatible" content="IE=edge">
                <title>%s</title>
                <!--[if mso]>
                <noscript>
                    <xml>
                        <o:OfficeDocumentSettings>
                            <o:PixelsPerInch>96</o:PixelsPerInch>
                        </o:OfficeDocumentSettings>
                    </xml>
                </noscript>
                <![endif]-->
            </head>
            <body style="margin: 0; padding: 0; background-color: #F5F0E8; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #F5F0E8;">
                    <tr>
                        <td style="padding: 24px 16px;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto;">
                                <!-- Header -->
                                <tr>
                                    <td style="background-color: #2D5A47; padding: 24px 32px; border-radius: 8px 8px 0 0; text-align: center;">
                                        <h1 style="color: #FFFFFF; font-family: 'Georgia', serif; font-size: 28px; font-weight: 400; margin: 0; letter-spacing: 1px;">
                                            Forever Home
                                        </h1>
                                        <p style="color: #E8E4DC; font-size: 12px; margin: 8px 0 0 0; letter-spacing: 0.5px;">
                                            Finding forever families for pets
                                        </p>
                                    </td>
                                </tr>
                                <!-- Content -->
                                <tr>
                                    <td style="background-color: #FFFDF8; padding: 32px; border-left: 1px solid #E8E4DC; border-right: 1px solid #E8E4DC;">
                                        %s
                                    </td>
                                </tr>
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #E8E4DC; padding: 24px 32px; border-radius: 0 0 8px 8px; text-align: center;">
                                        <p style="color: #5C5C5C; font-size: 12px; margin: 0 0 8px 0;">
                                            Forever Home - Connecting pets with loving families
                                        </p>
                                        <p style="color: #8C8C8C; font-size: 11px; margin: 0;">
                                            You received this email because you have an account with Forever Home.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(escapeHtml(title), content);
    }

    /**
     * Escape HTML special characters to prevent XSS.
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
