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
    private final RescueOrganizationRepository rescueOrganizationRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               RescueOrganizationRepository rescueOrganizationRepository,
                               EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
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

    /**
     * Marks a notification as read, verifying that it belongs to the specified user.
     * @param notificationId the notification ID
     * @param userId the user ID (for ownership verification)
     * @throws IllegalArgumentException if the notification doesn't exist or doesn't belong to the user
     */
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findByIdAndUserId(notificationId, userId).ifPresentOrElse(
            notification -> {
                notification.markAsRead();
                notificationRepository.save(notification);
            },
            () -> {
                logger.warn("Attempted to mark notification {} as read by user {} - not found or unauthorized",
                    notificationId, userId);
                throw new IllegalArgumentException("Notification not found");
            }
        );
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
                true, // status change
                "/pets/" + pet.getId()
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
                true, // status change
                "/pets/" + pet.getId()
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
                true, // status change
                "/pets/" + pet.getId()
        );
    }

    /**
     * @deprecated This method contradicts the system design. Vets are not assigned to pets;
     * instead, fosters bring pets to vets of their choice, and vets look up pets by microchip.
     * This method should not be used and will be removed in a future release.
     */
    @Deprecated(forRemoval = true)
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
                true, // status change
                "/pets/" + pet.getId()
        );
    }

    public void notifyNewApplication(UUID rescueOrgId, UUID petId, String petName) {
        // Look up the user ID from the rescue organization profile
        UUID rescueOrgUserId = rescueOrganizationRepository.findById(rescueOrgId)
                .map(RescueOrganization::getUserId)
                .orElse(null);
        if (rescueOrgUserId == null) {
            logger.warn("Cannot send notification: rescue org {} not found", rescueOrgId);
            return;
        }

        String title = "New Application";
        String message = "A new adoption application has been submitted for '" + petName + "'.";
        createNotificationWithEmail(
                rescueOrgUserId,
                NotificationType.NEW_APPLICATION,
                title,
                message,
                false, // new application
                "/pets/" + petId
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
        notifyFavoriteAvailable(adopterId, petName, null);
    }

    public void notifyFavoriteAvailable(UUID adopterId, String petName, UUID petId) {
        String title = "Favorite Pet Available";
        String message = "A pet you favorited, '" + petName + "', is now available for adoption!";
        String link = petId != null ? "/pets/" + petId : null;

        // Get user preferences to check favorite updates
        User user = userRepository.findById(adopterId).orElse(null);
        if (user == null) {
            logger.warn("Cannot send notification: user {} not found", adopterId);
            return;
        }

        // Create in-app notification if enabled
        NotificationPreferences prefs = user.getNotificationPreferences();
        if (prefs == null || prefs.inAppEnabled()) {
            Notification notification = Notification.create(adopterId, NotificationType.FAVORITE_UPDATE, title, message, link);
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

    // ==================== NEW NOTIFICATION METHODS ====================

    /**
     * Notify adopter that their application was submitted successfully.
     */
    public void notifyAdopterApplicationSubmitted(UUID adopterUserId, String petName) {
        String title = "Application Submitted";
        String message = "Your application to adopt '" + petName + "' has been submitted successfully. The rescue organization will review your application soon.";
        createNotificationWithEmail(
                adopterUserId,
                NotificationType.APPLICATION_UPDATE,
                title,
                message,
                true // status change
        );
    }

    /**
     * Notify foster that someone has applied to adopt their pet.
     */
    public void notifyFosterApplicationReceived(UUID fosterUserId, String petName) {
        String title = "New Application for Your Pet";
        String message = "Someone has applied to adopt your pet '" + petName + "'. The rescue organization will review the application.";
        createNotificationWithEmail(
                fosterUserId,
                NotificationType.NEW_APPLICATION,
                title,
                message,
                false // new application type
        );
    }

    /**
     * Notify foster that an application for their pet was approved.
     */
    public void notifyFosterApplicationApproved(UUID fosterUserId, String petName) {
        String title = "Adoption Application Approved";
        String message = "An application to adopt your pet '" + petName + "' has been approved. The adoption process is now underway.";
        createNotificationWithEmail(
                fosterUserId,
                NotificationType.APPLICATION_UPDATE,
                title,
                message,
                true // status change
        );
    }

    /**
     * Notify foster that the adoption of their pet is complete.
     */
    public void notifyFosterAdoptionComplete(UUID fosterUserId, String petName) {
        String title = "Adoption Complete!";
        String message = "Congratulations! Your pet '" + petName + "' has found their forever home. Thank you for helping them on their journey.";
        createNotificationWithEmail(
                fosterUserId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true // status change
        );
    }

    /**
     * Notify rescue org that a pet has been reassigned to a different rescue organization.
     */
    public void notifyRescueOrgPetReassigned(UUID rescueOrgId, Pet pet) {
        // Look up the user ID from the rescue organization profile
        UUID rescueOrgUserId = rescueOrganizationRepository.findById(rescueOrgId)
                .map(RescueOrganization::getUserId)
                .orElse(null);
        if (rescueOrgUserId == null) {
            logger.warn("Cannot send notification: rescue org {} not found", rescueOrgId);
            return;
        }

        String title = "Pet Reassigned";
        String message = "Pet '" + pet.getName() + "' has been reassigned to a different rescue organization by the foster.";
        createNotificationWithEmail(
                rescueOrgUserId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true, // status change
                "/pets/" + pet.getId()
        );
    }

    /**
     * Notify rescue org that a foster has withdrawn their pet.
     */
    public void notifyRescueOrgPetWithdrawn(UUID rescueOrgUserId, Pet pet) {
        String title = "Pet Withdrawn";
        String message = "The foster has withdrawn pet '" + pet.getName() + "' from the adoption process.";
        createNotificationWithEmail(
                rescueOrgUserId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true, // status change
                "/pets/" + pet.getId()
        );
    }

    /**
     * Notify rescue org that their organization has been verified by admin.
     */
    public void notifyRescueOrgVerified(UUID rescueOrgUserId) {
        String title = "Organization Verified!";
        String message = "Congratulations! Your rescue organization has been verified by Forever Home. You can now accept pets from fosters and manage adoptions.";
        createNotificationWithEmail(
                rescueOrgUserId,
                NotificationType.SYSTEM_ALERT,
                title,
                message,
                true // status change
        );
    }

    /**
     * Notify rescue org that a vet has signed off on one of their pets.
     */
    public void notifyRescueOrgVetApproved(UUID rescueOrgUserId, String petName) {
        String title = "Vet Sign-off Complete";
        String message = "Pet '" + petName + "' has received vet sign-off and is now available for adoption.";
        createNotificationWithEmail(
                rescueOrgUserId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true // status change
        );
    }

    /**
     * Notify rescue org that a vet has declined to sign off on one of their pets.
     */
    public void notifyRescueOrgVetDeclined(UUID rescueOrgUserId, String petName, String reason) {
        String title = "Vet Sign-off Declined";
        String message = "Pet '" + petName + "' was not approved by the vet.";
        if (reason != null && !reason.isBlank()) {
            message += " Reason: " + reason;
        }
        createNotificationWithEmail(
                rescueOrgUserId,
                NotificationType.PET_STATUS_CHANGE,
                title,
                message,
                true // status change
        );
    }

    /**
     * Notify rescue org that their application has been rejected by admin.
     */
    public void notifyRescueOrgRejected(UUID rescueOrgUserId, String reason) {
        String title = "Organization Application Declined";
        String message = "Your rescue organization application has been declined.";
        if (reason != null && !reason.isBlank()) {
            message += " Reason: " + reason;
        }
        createNotificationWithEmail(
                rescueOrgUserId,
                NotificationType.SYSTEM_ALERT,
                title,
                message,
                true // status change
        );
    }

    /**
     * Notify adopter that their application is under review.
     */
    public void notifyAdopterApplicationUnderReview(UUID adopterUserId, String petName) {
        String title = "Application Under Review";
        String message = "Your application to adopt '" + petName + "' is now being reviewed by the rescue organization.";
        createNotificationWithEmail(
                adopterUserId,
                NotificationType.APPLICATION_UPDATE,
                title,
                message,
                true // status change
        );
    }

    /**
     * Notify adopter that a pet they favorited has been adopted.
     */
    public void notifyFavoritePetAdopted(UUID adopterUserId, String petName, UUID petId) {
        String title = "Favorited Pet Adopted";
        String message = "A pet you favorited, '" + petName + "', has been adopted and found their forever home.";
        String link = petId != null ? "/pets/" + petId : null;

        // Get user preferences to check favorite updates
        User user = userRepository.findById(adopterUserId).orElse(null);
        if (user == null) {
            logger.warn("Cannot send notification: user {} not found", adopterUserId);
            return;
        }

        // Create in-app notification if enabled
        NotificationPreferences prefs = user.getNotificationPreferences();
        if (prefs == null || prefs.inAppEnabled()) {
            Notification notification = Notification.create(adopterUserId, NotificationType.FAVORITE_UPDATE, title, message, link);
            notificationRepository.save(notification);
        }

        // Send email if favorite updates enabled
        if (prefs != null && prefs.emailFavoriteUpdates()) {
            sendNotificationEmail(user.getEmail(), title, message);
        }
    }

    /**
     * Notify all admins that a new rescue org is awaiting approval.
     */
    public void notifyAdminsPendingApproval(String rescueOrgName) {
        List<User> admins = userRepository.findByRole(com.example.foreverhome.domain.user.UserRole.ADMIN);
        for (User admin : admins) {
            String title = "New Approval Request";
            String message = "Rescue organization '" + rescueOrgName + "' is awaiting approval.";
            createNotificationWithEmail(
                    admin.getId(),
                    NotificationType.SYSTEM_ALERT,
                    title,
                    message,
                    true // status change
            );
        }
    }

    /**
     * Creates an in-app notification and optionally sends an email based on user preferences.
     */
    private void createNotificationWithEmail(UUID userId, NotificationType type, String title, String message, boolean isStatusChange) {
        createNotificationWithEmail(userId, type, title, message, isStatusChange, null);
    }

    /**
     * Creates an in-app notification with a deep link and optionally sends an email based on user preferences.
     */
    private void createNotificationWithEmail(UUID userId, NotificationType type, String title, String message, boolean isStatusChange, String link) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.warn("Cannot send notification: user {} not found", userId);
            return;
        }

        NotificationPreferences prefs = user.getNotificationPreferences();

        // Create in-app notification if enabled
        if (prefs == null || prefs.inAppEnabled()) {
            Notification notification = Notification.create(userId, type, title, message, link);
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
