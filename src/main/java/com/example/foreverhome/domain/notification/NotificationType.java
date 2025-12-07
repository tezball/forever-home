package com.example.foreverhome.domain.notification;

/**
 * Represents the type of notification sent to users.
 */
public enum NotificationType {
    PET_STATUS_CHANGE("Pet Status Change"),
    NEW_APPLICATION("New Application"),
    APPLICATION_UPDATE("Application Update"),
    FAVORITE_UPDATE("Favorite Update"),
    SYSTEM_ALERT("System Alert");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this notification type.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this notification type can be sent via email.
     * Currently all notification types are email eligible.
     * @return true if the notification can be sent via email
     */
    public boolean isEmailEligible() {
        return true;
    }
}
