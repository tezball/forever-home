package com.example.foreverhome.domain.user;

/**
 * Value object representing user notification preferences.
 * Controls which notifications the user wants to receive.
 */
public record NotificationPreferences(
        boolean emailStatusChanges,
        boolean emailNewApplications,
        boolean emailFavoriteUpdates,
        boolean inAppEnabled
) {
    /**
     * Creates default preferences with all notifications enabled.
     * @return default NotificationPreferences
     */
    public static NotificationPreferences defaults() {
        return new NotificationPreferences(true, true, true, true);
    }

    /**
     * Checks if any email notification is enabled.
     * @return true if at least one email preference is enabled
     */
    public boolean hasAnyEmailEnabled() {
        return emailStatusChanges || emailNewApplications || emailFavoriteUpdates;
    }

    /**
     * Returns a new instance with emailStatusChanges updated.
     * @param enabled the new value
     * @return new NotificationPreferences instance
     */
    public NotificationPreferences withEmailStatusChanges(boolean enabled) {
        return new NotificationPreferences(enabled, emailNewApplications, emailFavoriteUpdates, inAppEnabled);
    }

    /**
     * Returns a new instance with emailNewApplications updated.
     * @param enabled the new value
     * @return new NotificationPreferences instance
     */
    public NotificationPreferences withEmailNewApplications(boolean enabled) {
        return new NotificationPreferences(emailStatusChanges, enabled, emailFavoriteUpdates, inAppEnabled);
    }

    /**
     * Returns a new instance with emailFavoriteUpdates updated.
     * @param enabled the new value
     * @return new NotificationPreferences instance
     */
    public NotificationPreferences withEmailFavoriteUpdates(boolean enabled) {
        return new NotificationPreferences(emailStatusChanges, emailNewApplications, enabled, inAppEnabled);
    }

    /**
     * Returns a new instance with inAppEnabled updated.
     * @param enabled the new value
     * @return new NotificationPreferences instance
     */
    public NotificationPreferences withInAppEnabled(boolean enabled) {
        return new NotificationPreferences(emailStatusChanges, emailNewApplications, emailFavoriteUpdates, enabled);
    }

    /**
     * Returns a new instance with all email notifications disabled.
     * @return new NotificationPreferences instance with emails disabled
     */
    public NotificationPreferences disableAllEmails() {
        return new NotificationPreferences(false, false, false, inAppEnabled);
    }
}
