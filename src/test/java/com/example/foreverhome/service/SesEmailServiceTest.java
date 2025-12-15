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

    private SesEmailService sesEmailService;

    @BeforeEach
    void setUp() {
        sesEmailService = new SesEmailService(sesClient, "noreply@foreverhome.local",
                "http://localhost:5173", metricsService, journeyLogger);
    }

    @Nested
    @DisplayName("sendVerificationEmail")
    class SendVerificationEmail {

        @Test
        @DisplayName("given email and token, when sendVerificationEmail, then sends email via SES")
        void givenEmailAndToken_whenSendVerificationEmail_thenSendsEmailViaSes() {
            // Given
            String to = "test@example.com";
            String token = "verification-token-123";
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            sesEmailService.sendVerificationEmail(to, token);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertThat(request.destination().toAddresses()).contains(to);
            assertThat(request.message().subject().data()).contains("Verify");
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
            when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

            // When
            sesEmailService.sendNotificationEmail(to, subject, body);

            // Then
            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertThat(request.message().subject().data()).isEqualTo(subject);
            assertThat(request.message().body().text().data()).isEqualTo(body);

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
}
