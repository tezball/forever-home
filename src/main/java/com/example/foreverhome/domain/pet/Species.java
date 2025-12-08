package com.example.foreverhome.domain.pet;

/**
 * Represents the species of a pet.
 * Forever Home specializes in cats and dogs only.
 */
public enum Species {
    DOG("Dog"),
    CAT("Cat");

    private final String displayName;

    Species(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this species.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
