package com.example.foreverhome.service;

import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.logging.UserJourneyLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsoleEmailService")
class ConsoleEmailServiceTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private UserJourneyLogger journeyLogger;

    private ConsoleEmailService consoleEmailService;

    @BeforeEach
    void setUp() {
        consoleEmailService = new ConsoleEmailService("http://localhost:5173", metricsService, journeyLogger);
    }

    @Nested
    @DisplayName("sendVerificationEmail")
    class SendVerificationEmail {

        @Test
        @DisplayName("given email and token, when sendVerificationEmail, then logs and records metrics")
        void givenEmailAndToken_whenSendVerificationEmail_thenLogsAndRecordsMetrics() {
            // Given
            String to = "test@example.com";
            String token = "verification-token-123";

            // When
            consoleEmailService.sendVerificationEmail(to, token);

            // Then
            verify(metricsService).recordEmailSent("verification");
            verify(journeyLogger).logEmail(eq(UserJourneyLogger.ACTION_EMAIL_SENT), eq("verification"), eq(to), eq(true), anyString());
        }
    }

    @Nested
    @DisplayName("sendPasswordResetEmail")
    class SendPasswordResetEmail {

        @Test
        @DisplayName("given email and token, when sendPasswordResetEmail, then logs and records metrics")
        void givenEmailAndToken_whenSendPasswordResetEmail_thenLogsAndRecordsMetrics() {
            // Given
            String to = "test@example.com";
            String token = "reset-token-456";

            // When
            consoleEmailService.sendPasswordResetEmail(to, token);

            // Then
            verify(metricsService).recordEmailSent("password_reset");
            verify(journeyLogger).logEmail(eq(UserJourneyLogger.ACTION_EMAIL_SENT), eq("password_reset"), eq(to), eq(true), anyString());
        }
    }

    @Nested
    @DisplayName("sendPasswordResetByAdmin")
    class SendPasswordResetByAdmin {

        @Test
        @DisplayName("given email and temp password, when sendPasswordResetByAdmin, then logs and records metrics")
        void givenEmailAndTempPassword_whenSendPasswordResetByAdmin_thenLogsAndRecordsMetrics() {
            // Given
            String to = "test@example.com";
            String temporaryPassword = "TempPass123!";

            // When
            consoleEmailService.sendPasswordResetByAdmin(to, temporaryPassword);

            // Then
            verify(metricsService).recordEmailSent("admin_password_reset");
            verify(journeyLogger).logEmail(eq(UserJourneyLogger.ACTION_EMAIL_SENT), eq("admin_password_reset"), eq(to), eq(true), anyString());
        }
    }

    @Nested
    @DisplayName("sendNotificationEmail")
    class SendNotificationEmail {

        @Test
        @DisplayName("given email, subject and body, when sendNotificationEmail, then logs and records metrics")
        void givenEmailSubjectAndBody_whenSendNotificationEmail_thenLogsAndRecordsMetrics() {
            // Given
            String to = "test@example.com";
            String subject = "Test Notification";
            String body = "This is a test notification body.";

            // When
            consoleEmailService.sendNotificationEmail(to, subject, body);

            // Then
            verify(metricsService).recordEmailSent("notification");
            verify(journeyLogger).logEmail(eq(UserJourneyLogger.ACTION_EMAIL_SENT), eq("notification"), eq(to), eq(true), contains("Subject:"));
        }
    }

    @Nested
    @DisplayName("sendWelcomeEmail")
    class SendWelcomeEmail {

        @Test
        @DisplayName("given email, name, and role, when sendWelcomeEmail, then logs and records metrics")
        void givenEmailNameAndRole_whenSendWelcomeEmail_thenLogsAndRecordsMetrics() {
            // Given
            String to = "test@example.com";
            String name = "John Doe";
            UserRole role = UserRole.ADOPTER;

            // When
            consoleEmailService.sendWelcomeEmail(to, name, role);

            // Then
            verify(metricsService).recordEmailSent("welcome");
            verify(journeyLogger).logEmail(eq(UserJourneyLogger.ACTION_EMAIL_SENT), eq("welcome"), eq(to), eq(true), contains(role.toString()));
        }
    }
}
