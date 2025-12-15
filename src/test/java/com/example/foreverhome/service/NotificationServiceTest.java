package com.example.foreverhome.service;

import com.example.foreverhome.domain.notification.Notification;
import com.example.foreverhome.domain.notification.NotificationType;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.user.NotificationPreferences;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.repository.NotificationRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @Mock
    private EmailService emailService;

    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private UUID userId;
    private User mockUser;
    private Pet mockPet;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                rescueOrganizationRepository,
                emailService
        );
        userId = UUID.randomUUID();
        mockUser = mock(User.class);
        mockPet = mock(Pet.class);
        when(mockPet.getName()).thenReturn("Buddy");
    }

    @Nested
    @DisplayName("getNotificationsForUser")
    class GetNotificationsForUser {

        @Test
        @DisplayName("should return notifications ordered by created date desc")
        void shouldReturnNotificationsOrderedByCreatedDateDesc() {
            List<Notification> notifications = List.of(mock(Notification.class), mock(Notification.class));
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(notifications);

            List<Notification> result = notificationService.getNotificationsForUser(userId);

            assertThat(result).hasSize(2);
            verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId);
        }
    }

    @Nested
    @DisplayName("getUnreadNotificationsForUser")
    class GetUnreadNotificationsForUser {

        @Test
        @DisplayName("should return only unread notifications")
        void shouldReturnOnlyUnreadNotifications() {
            List<Notification> unreadNotifications = List.of(mock(Notification.class));
            when(notificationRepository.findUnreadByUserId(userId)).thenReturn(unreadNotifications);

            List<Notification> result = notificationService.getUnreadNotificationsForUser(userId);

            assertThat(result).hasSize(1);
            verify(notificationRepository).findUnreadByUserId(userId);
        }
    }

    @Nested
    @DisplayName("countUnreadForUser")
    class CountUnreadForUser {

        @Test
        @DisplayName("should return count of unread notifications")
        void shouldReturnCountOfUnreadNotifications() {
            when(notificationRepository.countUnreadByUserId(userId)).thenReturn(5);

            int result = notificationService.countUnreadForUser(userId);

            assertThat(result).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("should mark notification as read")
        void shouldMarkNotificationAsRead() {
            UUID notificationId = UUID.randomUUID();
            Notification notification = mock(Notification.class);
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

            notificationService.markAsRead(notificationId);

            verify(notification).markAsRead();
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should do nothing when notification not found")
        void shouldDoNothingWhenNotificationNotFound() {
            UUID notificationId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            notificationService.markAsRead(notificationId);

            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("should mark all notifications as read for user")
        void shouldMarkAllNotificationsAsReadForUser() {
            notificationService.markAllAsRead(userId);

            verify(notificationRepository).markAllAsReadByUserId(userId);
        }
    }

    @Nested
    @DisplayName("notifyRescueOrgPetSubmitted")
    class NotifyRescueOrgPetSubmitted {

        @Test
        @DisplayName("should create notification when user found with in-app enabled")
        void shouldCreateNotificationWhenUserFoundWithInAppEnabled() {
            NotificationPreferences prefs = new NotificationPreferences(true, true, true, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(mockUser.getEmail()).thenReturn("rescue@test.com");
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyRescueOrgPetSubmitted(userId, mockPet);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getTitle()).isEqualTo("New Pet Submitted");
            assertThat(savedNotification.getMessage()).contains("Buddy");
        }

        @Test
        @DisplayName("should not create notification when user not found")
        void shouldNotCreateNotificationWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            notificationService.notifyRescueOrgPetSubmitted(userId, mockPet);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should send email when status changes preference enabled")
        void shouldSendEmailWhenStatusChangesPreferenceEnabled() {
            NotificationPreferences prefs = new NotificationPreferences(true, true, true, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(mockUser.getEmail()).thenReturn("rescue@test.com");
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyRescueOrgPetSubmitted(userId, mockPet);

            verify(emailService).sendNotificationEmail(eq("rescue@test.com"), contains("Forever Home"), anyString());
        }
    }

    @Nested
    @DisplayName("notifyFosterPetAccepted")
    class NotifyFosterPetAccepted {

        @Test
        @DisplayName("should create notification with correct message")
        void shouldCreateNotificationWithCorrectMessage() {
            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(true, false, false, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyFosterPetAccepted(userId, mockPet);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getTitle()).isEqualTo("Pet Accepted");
            assertThat(savedNotification.getMessage()).contains("accepted by the rescue organization");
        }
    }

    @Nested
    @DisplayName("notifyFosterPetDeclined")
    class NotifyFosterPetDeclined {

        @Test
        @DisplayName("should include reason in notification message")
        void shouldIncludeReasonInNotificationMessage() {
            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(true, false, false, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyFosterPetDeclined(userId, mockPet, "Missing vaccination records");

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getTitle()).isEqualTo("Pet Declined");
            assertThat(savedNotification.getMessage()).contains("Missing vaccination records");
        }
    }

    @Nested
    @DisplayName("notifyNewApplication")
    class NotifyNewApplication {

        @Test
        @DisplayName("should create notification for new application")
        void shouldCreateNotificationForNewApplication() {
            // Create a rescue org profile ID and mock the lookup
            UUID rescueOrgProfileId = UUID.randomUUID();
            RescueOrganization mockRescueOrg = mock(RescueOrganization.class);
            when(mockRescueOrg.getUserId()).thenReturn(userId);
            when(rescueOrganizationRepository.findById(rescueOrgProfileId)).thenReturn(Optional.of(mockRescueOrg));

            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(false, true, false, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(mockUser.getEmail()).thenReturn("rescue@test.com");
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            UUID petId = UUID.randomUUID();
            notificationService.notifyNewApplication(rescueOrgProfileId, petId, "Buddy");

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getType()).isEqualTo(NotificationType.NEW_APPLICATION);
            assertThat(savedNotification.getMessage()).contains("Buddy");
        }

        @Test
        @DisplayName("should send email when new applications preference enabled")
        void shouldSendEmailWhenNewApplicationsPreferenceEnabled() {
            // Create a rescue org profile ID and mock the lookup
            UUID rescueOrgProfileId = UUID.randomUUID();
            RescueOrganization mockRescueOrg = mock(RescueOrganization.class);
            when(mockRescueOrg.getUserId()).thenReturn(userId);
            when(rescueOrganizationRepository.findById(rescueOrgProfileId)).thenReturn(Optional.of(mockRescueOrg));

            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(false, true, false, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(mockUser.getEmail()).thenReturn("rescue@test.com");
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyNewApplication(rescueOrgProfileId, UUID.randomUUID(), "Buddy");

            verify(emailService).sendNotificationEmail(eq("rescue@test.com"), contains("Forever Home"), anyString());
        }
    }

    @Nested
    @DisplayName("notifyApplicationStatusChange")
    class NotifyApplicationStatusChange {

        @Test
        @DisplayName("should create notification for approved application")
        void shouldCreateNotificationForApprovedApplication() {
            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(true, true, false, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(mockUser.getEmail()).thenReturn("adopter@test.com");
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyApplicationStatusChange(userId, "Buddy", "Approved");

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getTitle()).isEqualTo("Application Approved");
            assertThat(savedNotification.getType()).isEqualTo(NotificationType.APPLICATION_UPDATE);
        }

        @Test
        @DisplayName("should create notification for rejected application")
        void shouldCreateNotificationForRejectedApplication() {
            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(true, false, false, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyApplicationStatusChange(userId, "Buddy", "Rejected");

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getTitle()).isEqualTo("Application Rejected");
        }
    }

    @Nested
    @DisplayName("notifyFavoriteAvailable")
    class NotifyFavoriteAvailable {

        @Test
        @DisplayName("should create notification when favorite updates enabled")
        void shouldCreateNotificationWhenFavoriteUpdatesEnabled() {
            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(false, false, true, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(mockUser.getEmail()).thenReturn("adopter@test.com");
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyFavoriteAvailable(userId, "Buddy");

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getType()).isEqualTo(NotificationType.FAVORITE_UPDATE);
            assertThat(savedNotification.getMessage()).contains("Buddy");
        }

        @Test
        @DisplayName("should send email when email favorite updates enabled")
        void shouldSendEmailWhenEmailFavoriteUpdatesEnabled() {
            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(false, false, true, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(mockUser.getEmail()).thenReturn("adopter@test.com");
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyFavoriteAvailable(userId, "Buddy");

            verify(emailService).sendNotificationEmail(eq("adopter@test.com"), contains("Forever Home"), anyString());
        }

        @Test
        @DisplayName("should not send email when favorite updates disabled")
        void shouldNotSendEmailWhenFavoriteUpdatesDisabled() {
            NotificationPreferences prefs = new NotificationPreferences(true, false, false, false);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyFavoriteAvailable(userId, "Buddy");

            verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("notifyVetApproved")
    class NotifyVetApproved {

        @Test
        @DisplayName("should create notification with rescue org name")
        void shouldCreateNotificationWithRescueOrgName() {
            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(true, false, false, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyVetApproved(userId, "Happy Paws Rescue");

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getTitle()).isEqualTo("Vet Approval Granted");
            assertThat(savedNotification.getMessage()).contains("Happy Paws Rescue");
            assertThat(savedNotification.getType()).isEqualTo(NotificationType.SYSTEM_ALERT);
        }
    }

    @Nested
    @DisplayName("notifyVetApprovalRevoked")
    class NotifyVetApprovalRevoked {

        @Test
        @DisplayName("should create notification for revoked approval")
        void shouldCreateNotificationForRevokedApproval() {
            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            NotificationPreferences prefs = new NotificationPreferences(true, false, false, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            notificationService.notifyVetApprovalRevoked(userId, "Happy Paws Rescue");

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getTitle()).isEqualTo("Vet Approval Revoked");
            assertThat(savedNotification.getMessage()).contains("revoked");
        }
    }

    @Nested
    @DisplayName("Email Error Handling")
    class EmailErrorHandling {

        @Test
        @DisplayName("should not throw when email service fails")
        void shouldNotThrowWhenEmailServiceFails() {
            // Constructor order: emailStatusChanges, emailNewApplications, emailFavoriteUpdates, inAppEnabled
            // inAppEnabled=true to save notification, emailStatusChanges=true to send email
            NotificationPreferences prefs = new NotificationPreferences(true, false, false, true);
            when(mockUser.getNotificationPreferences()).thenReturn(prefs);
            when(mockUser.getEmail()).thenReturn("test@test.com");
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            doThrow(new RuntimeException("Email server unavailable"))
                    .when(emailService).sendNotificationEmail(anyString(), anyString(), anyString());

            // Should not throw
            notificationService.notifyFosterPetAccepted(userId, mockPet);

            // Notification should still be saved
            verify(notificationRepository).save(any());
        }
    }
}
