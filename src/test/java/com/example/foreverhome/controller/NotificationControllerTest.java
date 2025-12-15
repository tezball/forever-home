package com.example.foreverhome.controller;

import com.example.foreverhome.domain.notification.Notification;
import com.example.foreverhome.domain.notification.NotificationType;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private UserPrincipal userPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userPrincipal = new UserPrincipal(userId, UserRole.ADOPTER, true);
    }

    @Nested
    @DisplayName("GET /api/notifications")
    class GetNotifications {

        @Test
        @DisplayName("given user with notifications, when getNotifications, then returns list of notifications")
        void givenUserWithNotifications_whenGetNotifications_thenReturnsListOfNotifications() {
            // Given
            Notification notification1 = createNotification(userId, NotificationType.NEW_APPLICATION, false);
            Notification notification2 = createNotification(userId, NotificationType.APPLICATION_UPDATE, true);

            when(notificationService.getNotificationsForUser(userId)).thenReturn(List.of(notification1, notification2));

            // When
            ResponseEntity<List<Notification>> response = notificationController.getNotifications(userPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("given user with no notifications, when getNotifications, then returns empty list")
        void givenUserWithNoNotifications_whenGetNotifications_thenReturnsEmptyList() {
            // Given
            when(notificationService.getNotificationsForUser(userId)).thenReturn(List.of());

            // When
            ResponseEntity<List<Notification>> response = notificationController.getNotifications(userPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread")
    class GetUnreadNotifications {

        @Test
        @DisplayName("given user with unread notifications, when getUnreadNotifications, then returns only unread")
        void givenUserWithUnreadNotifications_whenGetUnreadNotifications_thenReturnsOnlyUnread() {
            // Given
            Notification unreadNotification = createNotification(userId, NotificationType.PET_STATUS_CHANGE, false);
            when(notificationService.getUnreadNotificationsForUser(userId)).thenReturn(List.of(unreadNotification));

            // When
            ResponseEntity<List<Notification>> response = notificationController.getUnreadNotifications(userPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).isRead()).isFalse();
        }

        @Test
        @DisplayName("given user with all read notifications, when getUnreadNotifications, then returns empty list")
        void givenUserWithAllReadNotifications_whenGetUnreadNotifications_thenReturnsEmptyList() {
            // Given
            when(notificationService.getUnreadNotificationsForUser(userId)).thenReturn(List.of());

            // When
            ResponseEntity<List<Notification>> response = notificationController.getUnreadNotifications(userPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread/count")
    class GetUnreadCount {

        @Test
        @DisplayName("given user with unread notifications, when getUnreadCount, then returns count")
        void givenUserWithUnreadNotifications_whenGetUnreadCount_thenReturnsCount() {
            // Given
            when(notificationService.countUnreadForUser(userId)).thenReturn(5);

            // When
            ResponseEntity<Map<String, Integer>> response = notificationController.getUnreadCount(userPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("count", 5);
        }

        @Test
        @DisplayName("given user with no unread notifications, when getUnreadCount, then returns zero")
        void givenUserWithNoUnreadNotifications_whenGetUnreadCount_thenReturnsZero() {
            // Given
            when(notificationService.countUnreadForUser(userId)).thenReturn(0);

            // When
            ResponseEntity<Map<String, Integer>> response = notificationController.getUnreadCount(userPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("count", 0);
        }
    }

    @Nested
    @DisplayName("PUT /api/notifications/{id}/read")
    class MarkAsRead {

        @Test
        @DisplayName("given valid notification ID, when markAsRead, then returns no content")
        void givenValidNotificationId_whenMarkAsRead_thenReturnsNoContent() {
            // Given
            UUID notificationId = UUID.randomUUID();
            doNothing().when(notificationService).markAsRead(notificationId);

            // When
            ResponseEntity<Void> response = notificationController.markAsRead(notificationId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(notificationService).markAsRead(notificationId);
        }
    }

    @Nested
    @DisplayName("PUT /api/notifications/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("given user with unread notifications, when markAllAsRead, then marks all as read")
        void givenUserWithUnreadNotifications_whenMarkAllAsRead_thenMarksAllAsRead() {
            // Given
            doNothing().when(notificationService).markAllAsRead(userId);

            // When
            ResponseEntity<Void> response = notificationController.markAllAsRead(userPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(notificationService).markAllAsRead(userId);
        }

        @Test
        @DisplayName("given user with no notifications, when markAllAsRead, then completes without error")
        void givenUserWithNoNotifications_whenMarkAllAsRead_thenCompletesWithoutError() {
            // Given
            doNothing().when(notificationService).markAllAsRead(userId);

            // When
            ResponseEntity<Void> response = notificationController.markAllAsRead(userPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    // Helper method
    private Notification createNotification(UUID userId, NotificationType type, boolean isRead) {
        Notification notification = Notification.create(userId, type, "Test Title", "Test message");
        if (isRead) {
            notification.markAsRead();
        }
        setId(notification, UUID.randomUUID());
        return notification;
    }

    private void setId(Object entity, UUID id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }
}
