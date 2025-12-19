package com.example.foreverhome.service;

import com.example.foreverhome.domain.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailTemplateService")
class EmailTemplateServiceTest {

    private EmailTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new EmailTemplateService("http://localhost:5173");
    }

    @Nested
    @DisplayName("generateVerificationEmail")
    class GenerateVerificationEmail {

        @Test
        @DisplayName("returns email content with correct subject")
        void returnsEmailContentWithCorrectSubject() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateVerificationEmail("John", "test-token");

            // Then
            assertThat(content.subject()).isEqualTo("Verify your Forever Home account");
        }

        @Test
        @DisplayName("includes verification link in HTML body")
        void includesVerificationLinkInHtmlBody() {
            // Given
            String token = "test-verification-token";

            // When
            EmailTemplateService.EmailContent content = templateService.generateVerificationEmail("John", token);

            // Then
            assertThat(content.htmlBody()).contains("http://localhost:5173/verify-email?token=" + token);
            assertThat(content.htmlBody()).contains("Verify Email Address");
        }

        @Test
        @DisplayName("includes verification link in text body")
        void includesVerificationLinkInTextBody() {
            // Given
            String token = "test-verification-token";

            // When
            EmailTemplateService.EmailContent content = templateService.generateVerificationEmail("John", token);

            // Then
            assertThat(content.textBody()).contains("http://localhost:5173/verify-email?token=" + token);
        }

        @Test
        @DisplayName("includes recipient name in content")
        void includesRecipientNameInContent() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateVerificationEmail("Jane Doe", "token");

            // Then
            assertThat(content.htmlBody()).contains("Jane Doe");
            assertThat(content.textBody()).contains("Jane Doe");
        }

        @Test
        @DisplayName("HTML body contains proper structure")
        void htmlBodyContainsProperStructure() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateVerificationEmail("John", "token");

            // Then
            assertThat(content.htmlBody()).contains("<!DOCTYPE html>");
            assertThat(content.htmlBody()).contains("<html");
            assertThat(content.htmlBody()).contains("</html>");
            assertThat(content.htmlBody()).contains("Forever Home");
        }

        @Test
        @DisplayName("HTML body contains brand colors")
        void htmlBodyContainsBrandColors() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateVerificationEmail("John", "token");

            // Then - Brand colors from UI style guide
            assertThat(content.htmlBody()).contains("#2D5A47"); // Forest (primary)
            assertThat(content.htmlBody()).contains("#F5F0E8"); // Warm Sand (background)
            assertThat(content.htmlBody()).contains("#FFFDF8"); // Cream (surface)
        }
    }

    @Nested
    @DisplayName("generatePasswordResetEmail")
    class GeneratePasswordResetEmail {

        @Test
        @DisplayName("returns email content with correct subject")
        void returnsEmailContentWithCorrectSubject() {
            // When
            EmailTemplateService.EmailContent content = templateService.generatePasswordResetEmail("John", "reset-token");

            // Then
            assertThat(content.subject()).isEqualTo("Reset your Forever Home password");
        }

        @Test
        @DisplayName("includes reset link in both HTML and text body")
        void includesResetLinkInBothBodies() {
            // Given
            String token = "reset-token-123";

            // When
            EmailTemplateService.EmailContent content = templateService.generatePasswordResetEmail("John", token);

            // Then
            String expectedLink = "http://localhost:5173/reset-password?token=" + token;
            assertThat(content.htmlBody()).contains(expectedLink);
            assertThat(content.textBody()).contains(expectedLink);
        }

        @Test
        @DisplayName("mentions 1 hour expiry")
        void mentionsOneHourExpiry() {
            // When
            EmailTemplateService.EmailContent content = templateService.generatePasswordResetEmail("John", "token");

            // Then
            assertThat(content.htmlBody()).contains("1 hour");
            assertThat(content.textBody()).contains("1 hour");
        }
    }

    @Nested
    @DisplayName("generateAdminPasswordResetEmail")
    class GenerateAdminPasswordResetEmail {

        @Test
        @DisplayName("returns email content with correct subject")
        void returnsEmailContentWithCorrectSubject() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateAdminPasswordResetEmail("John", "TempPass123!");

            // Then
            assertThat(content.subject()).isEqualTo("Your Forever Home password has been reset");
        }

        @Test
        @DisplayName("includes temporary password in body")
        void includesTemporaryPasswordInBody() {
            // Given
            String tempPassword = "TempPass123!";

            // When
            EmailTemplateService.EmailContent content = templateService.generateAdminPasswordResetEmail("John", tempPassword);

            // Then
            assertThat(content.htmlBody()).contains(tempPassword);
            assertThat(content.textBody()).contains(tempPassword);
        }

        @Test
        @DisplayName("includes login link")
        void includesLoginLink() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateAdminPasswordResetEmail("John", "TempPass123!");

            // Then
            assertThat(content.htmlBody()).contains("http://localhost:5173/login");
            assertThat(content.textBody()).contains("http://localhost:5173/login");
        }
    }

    @Nested
    @DisplayName("generateWelcomeEmail")
    class GenerateWelcomeEmail {

        @Test
        @DisplayName("returns email content with correct subject for all roles")
        void returnsEmailContentWithCorrectSubject() {
            for (UserRole role : UserRole.values()) {
                // When
                EmailTemplateService.EmailContent content = templateService.generateWelcomeEmail("John Doe", role);

                // Then
                assertThat(content.subject()).isEqualTo("Welcome to Forever Home!");
            }
        }

        @Test
        @DisplayName("includes recipient name for all roles")
        void includesRecipientName() {
            // Given
            String name = "Jane Smith";

            for (UserRole role : UserRole.values()) {
                // When
                EmailTemplateService.EmailContent content = templateService.generateWelcomeEmail(name, role);

                // Then
                assertThat(content.htmlBody()).contains(name);
                assertThat(content.textBody()).contains(name);
            }
        }

        @Test
        @DisplayName("foster email includes heartfelt message and register pet steps")
        void fosterEmailIncludesHeartfeltMessageAndRegisterPetSteps() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateWelcomeEmail("John", UserRole.FOSTER);

            // Then
            assertThat(content.htmlBody()).contains("Thank You for Opening Your Heart");
            assertThat(content.htmlBody()).contains("Register your pet");
            assertThat(content.htmlBody()).contains("With gratitude");
            assertThat(content.textBody()).contains("Register your pet");
        }

        @Test
        @DisplayName("adopter email includes journey message and browse pets steps")
        void adopterEmailIncludesJourneyMessageAndBrowsePetsSteps() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateWelcomeEmail("John", UserRole.ADOPTER);

            // Then
            assertThat(content.htmlBody()).contains("Your Journey to Finding a Friend");
            assertThat(content.htmlBody()).contains("Browse available pets");
            assertThat(content.htmlBody()).contains("Submit adoption applications");
            assertThat(content.textBody()).contains("Browse available pets");
        }

        @Test
        @DisplayName("vet email includes thank you and verification steps")
        void vetEmailIncludesThankYouAndVerificationSteps() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateWelcomeEmail("Dr. Smith", UserRole.VET);

            // Then
            assertThat(content.htmlBody()).contains("Thank You for Joining Our Network");
            assertThat(content.htmlBody()).contains("veterinary professional");
            assertThat(content.htmlBody()).contains("verification from a rescue");
            assertThat(content.textBody()).contains("verification from a rescue");
        }

        @Test
        @DisplayName("rescue org email includes network welcome and approval steps")
        void rescueOrgEmailIncludesNetworkWelcomeAndApprovalSteps() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateWelcomeEmail("Happy Tails Rescue", UserRole.RESCUE_ORG);

            // Then
            assertThat(content.htmlBody()).contains("Welcome to Our Rescue Network");
            assertThat(content.htmlBody()).contains("admin approval");
            assertThat(content.htmlBody()).contains("Verify veterinarians");
            assertThat(content.textBody()).contains("admin approval");
        }

        @Test
        @DisplayName("admin email includes admin access and dashboard steps")
        void adminEmailIncludesAdminAccessAndDashboardSteps() {
            // When
            EmailTemplateService.EmailContent content = templateService.generateWelcomeEmail("Admin User", UserRole.ADMIN);

            // Then
            assertThat(content.htmlBody()).contains("Admin Access Granted");
            assertThat(content.htmlBody()).contains("administrator access");
            assertThat(content.htmlBody()).contains("Approve rescue organization");
            assertThat(content.htmlBody()).contains("/admin");
            assertThat(content.textBody()).contains("/admin");
        }
    }

    @Nested
    @DisplayName("generateNotificationEmail")
    class GenerateNotificationEmail {

        @Test
        @DisplayName("returns email content with provided subject")
        void returnsEmailContentWithProvidedSubject() {
            // Given
            String subject = "Custom Notification Subject";

            // When
            EmailTemplateService.EmailContent content = templateService.generateNotificationEmail("John", subject, "Body text");

            // Then
            assertThat(content.subject()).isEqualTo(subject);
        }

        @Test
        @DisplayName("includes provided body content")
        void includesProvidedBodyContent() {
            // Given
            String bodyText = "This is the notification body content.";

            // When
            EmailTemplateService.EmailContent content = templateService.generateNotificationEmail("John", "Subject", bodyText);

            // Then
            assertThat(content.textBody()).contains(bodyText);
        }
    }

    @Nested
    @DisplayName("HTML escaping")
    class HtmlEscaping {

        @Test
        @DisplayName("escapes HTML special characters in recipient name")
        void escapesHtmlSpecialCharactersInRecipientName() {
            // Given - name with HTML special characters
            String maliciousName = "<script>alert('xss')</script>";

            // When
            EmailTemplateService.EmailContent content = templateService.generateVerificationEmail(maliciousName, "token");

            // Then - HTML should be escaped
            assertThat(content.htmlBody()).doesNotContain("<script>");
            assertThat(content.htmlBody()).contains("&lt;script&gt;");
        }
    }
}
