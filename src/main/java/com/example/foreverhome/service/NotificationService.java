package com.example.foreverhome.service;

import com.example.foreverhome.domain.notification.Notification;
import com.example.foreverhome.domain.notification.NotificationType;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
        createNotification(
                rescueOrgId,
                NotificationType.PET_STATUS_CHANGE,
                "New Pet Submitted",
                "A new pet '" + pet.getName() + "' has been submitted for review."
        );
    }

    public void notifyFosterPetAccepted(UUID fosterId, Pet pet) {
        createNotification(
                fosterId,
                NotificationType.PET_STATUS_CHANGE,
                "Pet Accepted",
                "Your pet '" + pet.getName() + "' has been accepted by the rescue organization."
        );
    }

    public void notifyFosterPetDeclined(UUID fosterId, Pet pet, String reason) {
        createNotification(
                fosterId,
                NotificationType.PET_STATUS_CHANGE,
                "Pet Declined",
                "Your pet '" + pet.getName() + "' was declined. Reason: " + reason
        );
    }

    public void notifyVetSignOffNeeded(UUID vetId, Pet pet) {
        createNotification(
                vetId,
                NotificationType.PET_STATUS_CHANGE,
                "Sign-off Required",
                "Pet '" + pet.getName() + "' needs your sign-off."
        );
    }

    public void notifyFosterPetAvailable(UUID fosterId, Pet pet) {
        createNotification(
                fosterId,
                NotificationType.PET_STATUS_CHANGE,
                "Pet Now Available",
                "Your pet '" + pet.getName() + "' is now available for adoption!"
        );
    }

    public void notifyNewApplication(UUID rescueOrgId, UUID petId, String petName) {
        createNotification(
                rescueOrgId,
                NotificationType.NEW_APPLICATION,
                "New Application",
                "A new adoption application has been submitted for '" + petName + "'."
        );
    }

    public void notifyApplicationStatusChange(UUID adopterId, String petName, String status) {
        createNotification(
                adopterId,
                NotificationType.APPLICATION_UPDATE,
                "Application " + status,
                "Your application for '" + petName + "' has been " + status.toLowerCase() + "."
        );
    }

    public void notifyFavoriteAvailable(UUID adopterId, String petName) {
        createNotification(
                adopterId,
                NotificationType.FAVORITE_UPDATE,
                "Favorite Pet Available",
                "A pet you favorited, '" + petName + "', is now available for adoption!"
        );
    }

    private void createNotification(UUID userId, NotificationType type, String title, String message) {
        Notification notification = Notification.create(userId, type, title, message);
        notificationRepository.save(notification);
    }
}
