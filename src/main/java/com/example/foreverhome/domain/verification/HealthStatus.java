package com.example.foreverhome.domain.verification;

/**
 * Represents the health status of a pet as determined by a veterinarian.
 */
public enum HealthStatus {
    GOOD("Good", false),
    KNOWN_CONDITIONS("Known Conditions", true);

    private final String displayName;
    private final boolean requiresHealthNotes;

    HealthStatus(String displayName, boolean requiresHealthNotes) {
        this.displayName = displayName;
        this.requiresHealthNotes = requiresHealthNotes;
    }

    /**
     * Gets the human-readable display name for this health status.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if health notes are required for this status.
     * Known conditions must be documented.
     * @return true if health notes are required
     */
    public boolean requiresHealthNotes() {
        return requiresHealthNotes;
    }
}
