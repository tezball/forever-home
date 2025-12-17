package com.example.foreverhome.service;

import com.example.foreverhome.logging.UserJourneyLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SesEmailService")
class SesEmailServiceTest {

    @Mock
    private SesClient sesClient;

    @Mock
    private MetricsService metricsService;

    @Mock
    private UserJourneyLogger journeyLogger;

    @Mock
    private EmailTemplateService templateService;

    private SesEmailService sesEmailService;

    @BeforeEach
    void setUp() {
        sesEmailService = new SesEmailService(
                sesClient,
                "noreply@foreverhome.local",
                "", // No configuration set for basic tests
                metricsService,
                journeyLogger,
                templateService);
    }

    @Nested
    @DisplayName("sendVerificationEmail")
    class SendVerificationEmail {

        @Test
        @DisplayName("given email and token, when sendVerificationEmail, then sends email via SES with HTML and text")
        void givenEmailAndToken_whenSendVerificationEmail_thenSendsEmailViaSes() {
            // Given
            String to = "test@example.com";
            String token = "verification-token-123";
            EmailTemplateService.EmailContent content = new EmailTemplateService.EmailContent(
                    "Verify your Forever Home account",
                    "<html>HTML body with token</html>",
                    "Plain text body with " + token
            );
            when(templateService.generateVerificationEmail(anyString(), eq(token))).thenReturn(content);
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            sesEmailService.sendVerificationEmail(to, token);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertThat(request.destination().toAddresses()).contains(to);
            assertThat(request.message().subject().data()).contains("Verify");
            assertThat(request.message().body().html().data()).contains("HTML body");
            assertThat(request.message().body().text().data()).contains(token);

            verify(metricsService).recordEmailSent("verification");
            verify(journeyLogger).logEmail(eq(UserJourneyLogger.ACTION_EMAIL_SENT), eq("verification"), eq(to), eq(true), anyString());
        }

        @Test
        @DisplayName("given SES fails, when sendVerificationEmail, then records failure without throwing")
        void givenSesFails_whenSendVerificationEmail_thenRecordsFailureWithoutThrowing() {
            // Given
            String to = "test@example.com";
            String token = "verification-token-123";
            EmailTemplateService.EmailContent content = new EmailTemplateService.EmailContent(
                    "Verify your Forever Home account",
                    "<html>HTML body</html>",
                    "Plain text body"
            );
            when(templateService.generateVerificationEmail(anyString(), eq(token))).thenReturn(content);
            when(sesClient.sendEmail(any(SendEmailRequest.class)))
                    .thenThrow(SesException.builder().message("SES error").build());

            // When - should not throw
            sesEmailService.sendVerificationEmail(to, token);

            // Then
            verify(metricsService).recordEmailFailed("verification");
            verify(journeyLogger).logEmail(eq(UserJourneyLogger.ACTION_EMAIL_FAILED), eq("verification"), eq(to), eq(false), anyString());
        }
    }

    @Nested
    @DisplayName("sendPasswordResetEmail")
    class SendPasswordResetEmail {

        @Test
        @DisplayName("given email and token, when sendPasswordResetEmail, then sends email with reset link")
        void givenEmailAndToken_whenSendPasswordResetEmail_thenSendsEmailWithResetLink() {
            // Given
            String to = "test@example.com";
            String token = "reset-token-456";
            EmailTemplateService.EmailContent content = new EmailTemplateService.EmailContent(
                    "Reset your Forever Home password",
                    "<html>Reset HTML body</html>",
                    "Reset text body with " + token
            );
            when(templateService.generatePasswordResetEmail(anyString(), eq(token))).thenReturn(content);
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            sesEmailService.sendPasswordResetEmail(to, token);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertThat(request.message().subject().data()).contains("Reset");

            verify(metricsService).recordEmailSent("password_reset");
        }
    }

    @Nested
    @DisplayName("sendPasswordResetByAdmin")
    class SendPasswordResetByAdmin {

        @Test
        @DisplayName("given email and temp password, when sendPasswordResetByAdmin, then sends email")
        void givenEmailAndTempPassword_whenSendPasswordResetByAdmin_thenSendsEmail() {
            // Given
            String to = "test@example.com";
            String temporaryPassword = "TempPass123!";
            EmailTemplateService.EmailContent content = new EmailTemplateService.EmailContent(
                    "Your Forever Home password has been reset",
                    "<html>Admin reset HTML body</html>",
                    "Admin reset text body with " + temporaryPassword
            );
            when(templateService.generateAdminPasswordResetEmail(anyString(), eq(temporaryPassword))).thenReturn(content);
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            sesEmailService.sendPasswordResetByAdmin(to, temporaryPassword);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertThat(request.message().body().text().data()).contains(temporaryPassword);

            verify(metricsService).recordEmailSent("password_reset");
        }
    }

    @Nested
    @DisplayName("sendNotificationEmail")
    class SendNotificationEmail {

        @Test
        @DisplayName("given email, subject and body, when sendNotificationEmail, then sends email")
        void givenEmailSubjectAndBody_whenSendNotificationEmail_thenSendsEmail() {
            // Given
            String to = "test@example.com";
            String subject = "Test notification from Forever Home";
            String body = "This is a test notification body.";
            EmailTemplateService.EmailContent content = new EmailTemplateService.EmailContent(
                    subject,
                    "<html>Notification HTML body</html>",
                    "Notification text body: " + body
            );
            when(templateService.generateNotificationEmail(anyString(), eq(subject), eq(body))).thenReturn(content);
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            sesEmailService.sendNotificationEmail(to, subject, body);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertThat(request.message().subject().data()).isEqualTo(subject);

            verify(metricsService).recordEmailSent("notification");
        }
    }

    @Nested
    @DisplayName("sendWelcomeEmail")
    class SendWelcomeEmail {

        @Test
        @DisplayName("given email and name, when sendWelcomeEmail, then sends personalized welcome email")
        void givenEmailAndName_whenSendWelcomeEmail_thenSendsPersonalizedWelcomeEmail() {
            // Given
            String to = "test@example.com";
            String name = "John Doe";
            EmailTemplateService.EmailContent content = new EmailTemplateService.EmailContent(
                    "Welcome to Forever Home!",
                    "<html>Welcome HTML body for " + name + "</html>",
                    "Welcome text body for " + name
            );
            when(templateService.generateWelcomeEmail(eq(name))).thenReturn(content);
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            sesEmailService.sendWelcomeEmail(to, name);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertThat(request.message().subject().data()).contains("Welcome");
            assertThat(request.message().body().text().data()).contains(name);

            verify(metricsService).recordEmailSent("welcome");
        }
    }

    @Nested
    @DisplayName("Configuration Set")
    class ConfigurationSetTests {

        @Test
        @DisplayName("given configuration set is configured, when sending email, then includes configuration set in request")
        void givenConfigurationSetConfigured_whenSendingEmail_thenIncludesConfigurationSetInRequest() {
            // Given - recreate service with configuration set
            String configSetName = "forever-home-dev-emails";
            SesEmailService serviceWithConfigSet = new SesEmailService(
                    sesClient,
                    "noreply@foreverhome.local",
                    configSetName,
                    metricsService,
                    journeyLogger,
                    templateService);

            String to = "test@example.com";
            String name = "John Doe";
            EmailTemplateService.EmailContent content = new EmailTemplateService.EmailContent(
                    "Welcome to Forever Home!",
                    "<html>Welcome</html>",
                    "Welcome text"
            );
            when(templateService.generateWelcomeEmail(eq(name))).thenReturn(content);
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            serviceWithConfigSet.sendWelcomeEmail(to, name);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertThat(request.configurationSetName()).isEqualTo(configSetName);
        }

        @Test
        @DisplayName("given configuration set is empty, when sending email, then does not include configuration set")
        void givenConfigurationSetEmpty_whenSendingEmail_thenDoesNotIncludeConfigurationSet() {
            // Given - service already has empty configuration set from setUp()
            String to = "test@example.com";
            String name = "John Doe";
            EmailTemplateService.EmailContent content = new EmailTemplateService.EmailContent(
                    "Welcome to Forever Home!",
                    "<html>Welcome</html>",
                    "Welcome text"
            );
            when(templateService.generateWelcomeEmail(eq(name))).thenReturn(content);
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            sesEmailService.sendWelcomeEmail(to, name);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertThat(request.configurationSetName()).isNull();
        }
    }

    @Nested
    @DisplayName("HTML and Text Body")
    class HtmlAndTextBody {

        @Test
        @DisplayName("given template with HTML and text, when sending email, then includes both in request")
        void givenTemplateWithHtmlAndText_whenSendingEmail_thenIncludesBothInRequest() {
            // Given
            String to = "test@example.com";
            String token = "test-token";
            String htmlBody = "<html><body><h1>Verify</h1></body></html>";
            String textBody = "Please verify your email";
            EmailTemplateService.EmailContent content = new EmailTemplateService.EmailContent(
                    "Verify your Forever Home account",
                    htmlBody,
                    textBody
            );
            when(templateService.generateVerificationEmail(anyString(), eq(token))).thenReturn(content);
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            sesEmailService.sendVerificationEmail(to, token);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();

            // Verify both HTML and text body are included
            assertThat(request.message().body().html()).isNotNull();
            assertThat(request.message().body().html().data()).isEqualTo(htmlBody);
            assertThat(request.message().body().text()).isNotNull();
            assertThat(request.message().body().text().data()).isEqualTo(textBody);

            // Verify UTF-8 charset is set
            assertThat(request.message().body().html().charset()).isEqualTo("UTF-8");
            assertThat(request.message().body().text().charset()).isEqualTo("UTF-8");
            assertThat(request.message().subject().charset()).isEqualTo("UTF-8");
        }
    }
}
