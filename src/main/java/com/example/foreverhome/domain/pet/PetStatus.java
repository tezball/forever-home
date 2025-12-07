package com.example.foreverhome.domain.pet;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the status of a pet in the adoption lifecycle.
 * The pet moves through these statuses as it progresses from registration to adoption.
 */
public enum PetStatus {
    DRAFT,
    PENDING_RESCUE,
    PENDING_VET,
    AVAILABLE,
    IN_PROGRESS,
    ADOPTED,
    WITHDRAWN,
    ON_HOLD;

    private static final Set<PetStatus> PUBLICLY_VISIBLE = EnumSet.of(AVAILABLE, IN_PROGRESS, ON_HOLD);
    private static final Set<PetStatus> EDITABLE = EnumSet.of(DRAFT, PENDING_RESCUE, PENDING_VET, AVAILABLE);
    private static final Set<PetStatus> TERMINAL = EnumSet.of(ADOPTED);

    /**
     * Checks if pets with this status are visible to the public.
     * Only AVAILABLE, IN_PROGRESS, and ON_HOLD pets are publicly visible.
     * @return true if publicly visible
     */
    public boolean isPubliclyVisible() {
        return PUBLICLY_VISIBLE.contains(this);
    }

    /**
     * Checks if pets with this status can be edited by the foster.
     * Editing is disabled once a pet reaches IN_PROGRESS or terminal statuses.
     * @return true if the pet can be edited
     */
    public boolean isEditable() {
        return EDITABLE.contains(this);
    }

    /**
     * Checks if this is a terminal status (no further transitions possible).
     * @return true if terminal
     */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /**
     * Checks if this status can transition to the target status.
     * @param target the target status
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(PetStatus target) {
        // All non-terminal statuses can transition to WITHDRAWN
        if (target == WITHDRAWN && !isTerminal()) {
            return true;
        }

        return switch (this) {
            case DRAFT -> target == PENDING_RESCUE;
            case PENDING_RESCUE -> target == PENDING_VET || target == DRAFT;
            case PENDING_VET -> target == AVAILABLE || target == PENDING_RESCUE;
            case AVAILABLE -> target == IN_PROGRESS || target == ON_HOLD;
            case IN_PROGRESS -> target == ADOPTED || target == AVAILABLE;
            case ON_HOLD -> target == AVAILABLE;
            case ADOPTED, WITHDRAWN -> false;
        };
    }
}
