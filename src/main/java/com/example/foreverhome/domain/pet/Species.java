package com.example.foreverhome.domain.pet;

/**
 * Represents the species of a pet.
 */
public enum Species {
    DOG("Dog"),
    CAT("Cat"),
    RABBIT("Rabbit"),
    BIRD("Bird"),
    OTHER("Other");

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
