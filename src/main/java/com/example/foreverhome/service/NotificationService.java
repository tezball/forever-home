package com.example.foreverhome.service;

import com.example.foreverhome.domain.notification.Notification;
import com.example.foreverhome.domain.notification.NotificationType;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.user.NotificationPreferences;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.repository.NotificationRepository;
import com.example.foreverhome.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public List<Notification> getNotificationsForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotificationsForUser(UUID userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    public int countUnreadForUser(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
    }

    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    public void notifyRescueOrgPetSubmitted(UUID rescueOrgId, Pet pet) {
        String title = "New Pet Submitted";
        String message = "A new pet '" + pet.getName() + "' has been submitted for review.";
        createNotificationWithEmail(
                rescueOrgId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true // status change
        );
    }

    public void notifyFosterPetAccepted(UUID fosterId, Pet pet) {
        String title = "Pet Accepted";
        String message = "Your pet '" + pet.getName() + "' has been accepted by the rescue organization.";
        createNotificationWithEmail(
                fosterId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true // status change
        );
    }

    public void notifyFosterPetDeclined(UUID fosterId, Pet pet, String reason) {
        String title = "Pet Declined";
        String message = "Your pet '" + pet.getName() + "' was declined. Reason: " + reason;
        createNotificationWithEmail(
                fosterId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true // status change
        );
    }

    public void notifyVetSignOffNeeded(UUID vetId, Pet pet) {
        String title = "Sign-off Required";
        String message = "Pet '" + pet.getName() + "' needs your sign-off.";
        createNotificationWithEmail(
                vetId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true // status change
        );
    }

    public void notifyFosterPetAvailable(UUID fosterId, Pet pet) {
        String title = "Pet Now Available";
        String message = "Your pet '" + pet.getName() + "' is now available for adoption!";
        createNotificationWithEmail(
                fosterId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true // status change
        );
    }

    public void notifyNewApplication(UUID rescueOrgId, UUID petId, String petName) {
        String title = "New Application";
        String message = "A new adoption application has been submitted for '" + petName + "'.";
        createNotificationWithEmail(
                rescueOrgId,
                NotificationType.NEW_APPLICATION,
                title,
                message,
                false // new application
        );
    }

    public void notifyApplicationStatusChange(UUID adopterId, String petName, String status) {
        String title = "Application " + status;
        String message = "Your application for '" + petName + "' has been " + status.toLowerCase() + ".";
        createNotificationWithEmail(
                adopterId,
                NotificationType.APPLICATION_UPDATE,
                title,
                message,
                true // status change
        );
    }

    public void notifyFavoriteAvailable(UUID adopterId, String petName) {
        String title = "Favorite Pet Available";
        String message = "A pet you favorited, '" + petName + "', is now available for adoption!";

        // Get user preferences to check favorite updates
        User user = userRepository.findById(adopterId).orElse(null);
        if (user == null) {
            logger.warn("Cannot send notification: user {} not found", adopterId);
            return;
        }

        // Create in-app notification if enabled
        NotificationPreferences prefs = user.getNotificationPreferences();
        if (prefs == null || prefs.inAppEnabled()) {
            Notification notification = Notification.create(adopterId, NotificationType.FAVORITE_UPDATE, title, message);
            notificationRepository.save(notification);
        }

        // Send email if favorite updates enabled
        if (prefs != null && prefs.emailFavoriteUpdates()) {
            sendNotificationEmail(user.getEmail(), title, message);
        }
    }

    public void notifyVetApproved(UUID vetUserId, String rescueOrgName) {
        String title = "Vet Approval Granted";
        String message = "You have been approved to verify pets for '" + rescueOrgName + "'.";
        createNotificationWithEmail(
                vetUserId,
                NotificationType.SYSTEM_ALERT,
                title,
                message,
                true // status change
        );
    }

    public void notifyVetApprovalRevoked(UUID vetUserId, String rescueOrgName) {
        String title = "Vet Approval Revoked";
        String message = "Your approval to verify pets for '" + rescueOrgName + "' has been revoked.";
        createNotificationWithEmail(
                vetUserId,
                NotificationType.SYSTEM_ALERT,
                title,
                message,
                true // status change
        );
    }

    public void notifyRescueOrgVetRequest(UUID rescueOrgUserId, String vetClinicName) {
        String title = "New Vet Approval Request";
        String message = "A vet from '" + vetClinicName + "' has requested approval to verify your pets.";
        createNotificationWithEmail(
                rescueOrgUserId,
                NotificationType.NEW_APPLICATION,
                title,
                message,
                false // new application type
        );
    }

    public void notifyVetRequestApproved(UUID vetUserId, String rescueOrgName) {
        String title = "Approval Request Accepted";
        String message = "Your request to be approved by '" + rescueOrgName + "' has been accepted. You can now verify their pets.";
        createNotificationWithEmail(
                vetUserId,
                NotificationType.APPLICATION_UPDATE,
                title,
                message,
                true // status change
        );
    }

    public void notifyVetRequestRejected(UUID vetUserId, String rescueOrgName, String reason) {
        String title = "Approval Request Declined";
        String message = "Your request to be approved by '" + rescueOrgName + "' has been declined.";
        if (reason != null && !reason.isBlank()) {
            message += " Reason: " + reason;
        }
        createNotificationWithEmail(
                vetUserId,
                NotificationType.APPLICATION_UPDATE,
                title,
                message,
                true // status change
        );
    }

    /**
     * Creates an in-app notification and optionally sends an email based on user preferences.
     */
    private void createNotificationWithEmail(UUID userId, NotificationType type, String title, String message, boolean isStatusChange) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.warn("Cannot send notification: user {} not found", userId);
            return;
        }

        NotificationPreferences prefs = user.getNotificationPreferences();

        // Create in-app notification if enabled
        if (prefs == null || prefs.inAppEnabled()) {
            Notification notification = Notification.create(userId, type, title, message);
            notificationRepository.save(notification);
        }

        // Determine if we should send email based on notification type and preferences
        boolean shouldSendEmail = false;
        if (prefs != null) {
            if (type == NotificationType.NEW_APPLICATION && prefs.emailNewApplications()) {
                shouldSendEmail = true;
            } else if (isStatusChange && prefs.emailStatusChanges()) {
                shouldSendEmail = true;
            }
        }

        if (shouldSendEmail) {
            sendNotificationEmail(user.getEmail(), title, message);
        }
    }

    /**
     * Sends a notification email to the user.
     */
    private void sendNotificationEmail(String email, String subject, String body) {
        try {
            emailService.sendNotificationEmail(email, "[Forever Home] " + subject, body);
            logger.debug("Sent notification email to {}: {}", email, subject);
        } catch (Exception e) {
            logger.error("Failed to send notification email to {}: {}", email, e.getMessage());
            // Don't throw - email failure shouldn't break the notification flow
        }
    }
}
