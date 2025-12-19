package com.example.foreverhome.service;

import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.logging.UserJourneyLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmtpEmailService")
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MetricsService metricsService;

    @Mock
    private UserJourneyLogger journeyLogger;

    private SmtpEmailService smtpEmailService;

    @BeforeEach
    void setUp() {
        smtpEmailService = new SmtpEmailService(mailSender, "noreply@foreverhome.local",
                "http://localhost:5173", metricsService, journeyLogger);
    }

    @Nested
    @DisplayName("sendVerificationEmail")
    class SendVerificationEmail {

        @Test
        @DisplayName("given email and token, when sendVerificationEmail, then sends email with verification link")
        void givenEmailAndToken_whenSendVerificationEmail_thenSendsEmailWithVerificationLink() {
            // Given
            String to = "test@example.com";
            String token = "verification-token-123";
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // When
            smtpEmailService.sendVerificationEmail(to, token);

            // Then
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            SimpleMailMessage message = captor.getValue();
            assertThat(message.getTo()).contains(to);
            assertThat(message.getSubject()).contains("Verify");
            assertThat(message.getText()).contains(token);

            verify(metricsService).recordEmailSent("verification");
            verify(journeyLogger).logEmail(eq(UserJourneyLogger.ACTION_EMAIL_SENT), eq("verification"), eq(to), eq(true), anyString());
        }

        @Test
        @DisplayName("given mail sender fails, when sendVerificationEmail, then records failure")
        void givenMailSenderFails_whenSendVerificationEmail_thenRecordsFailure() {
            // Given
            String to = "test@example.com";
            String token = "verification-token-123";
            doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

            // When
            smtpEmailService.sendVerificationEmail(to, token);

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
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // When
            smtpEmailService.sendPasswordResetEmail(to, token);

            // Then
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            SimpleMailMessage message = captor.getValue();
            assertThat(message.getTo()).contains(to);
            assertThat(message.getSubject()).contains("Reset");
            assertThat(message.getText()).contains(token);

            verify(metricsService).recordEmailSent("password_reset");
        }
    }

    @Nested
    @DisplayName("sendPasswordResetByAdmin")
    class SendPasswordResetByAdmin {

        @Test
        @DisplayName("given email and temp password, when sendPasswordResetByAdmin, then sends email with password")
        void givenEmailAndTempPassword_whenSendPasswordResetByAdmin_thenSendsEmailWithPassword() {
            // Given
            String to = "test@example.com";
            String temporaryPassword = "TempPass123!";
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // When
            smtpEmailService.sendPasswordResetByAdmin(to, temporaryPassword);

            // Then
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            SimpleMailMessage message = captor.getValue();
            assertThat(message.getText()).contains(temporaryPassword);

            verify(metricsService).recordEmailSent("admin_password_reset");
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
            String subject = "Test Notification";
            String body = "This is a test notification body.";
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // When
            smtpEmailService.sendNotificationEmail(to, subject, body);

            // Then
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            SimpleMailMessage message = captor.getValue();
            assertThat(message.getSubject()).isEqualTo(subject);
            assertThat(message.getText()).isEqualTo(body);

            verify(metricsService).recordEmailSent("notification");
        }
    }

    @Nested
    @DisplayName("sendWelcomeEmail")
    class SendWelcomeEmail {

        @Test
        @DisplayName("given email, name, and role, when sendWelcomeEmail, then sends personalized welcome email")
        void givenEmailNameAndRole_whenSendWelcomeEmail_thenSendsPersonalizedWelcomeEmail() {
            // Given
            String to = "test@example.com";
            String name = "John Doe";
            UserRole role = UserRole.FOSTER;
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // When
            smtpEmailService.sendWelcomeEmail(to, name, role);

            // Then
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            SimpleMailMessage message = captor.getValue();
            assertThat(message.getSubject()).contains("Welcome");
            assertThat(message.getText()).contains(name);

            verify(metricsService).recordEmailSent("welcome");
        }
    }
}
