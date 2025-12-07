package com.example.foreverhome.domain.pet;

/**
 * Represents the size category of a pet.
 */
public enum PetSize {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large");

    private final String displayName;

    PetSize(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this size.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
