package com.example.foreverhome.domain.pet;

/**
 * Represents the sex of a pet.
 */
public enum PetSex {
    MALE("Male"),
    FEMALE("Female");

    private final String displayName;

    PetSex(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this sex.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
