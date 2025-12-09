package com.example.foreverhome.domain.pet;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the history of status changes for a pet.
 */
@Table("pet_status_history")
public class PetStatusHistory implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("pet_id")
    private UUID petId;

    @Column("from_status")
    private PetStatus fromStatus;

    @Column("to_status")
    private PetStatus toStatus;

    @Column("changed_by")
    private UUID changedBy;

    @Column("changed_at")
    private Instant changedAt;

    @Column("notes")
    private String notes;

    @Transient
    private boolean isNew = false;

    protected PetStatusHistory() {
    }

    private PetStatusHistory(UUID id, UUID petId, PetStatus fromStatus, PetStatus toStatus,
                              UUID changedBy, Instant changedAt, String notes) {
        this.id = id;
        this.petId = petId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.notes = notes;
    }

    public static PetStatusHistory create(UUID petId, PetStatus fromStatus, PetStatus toStatus,
                                          UUID changedBy, String notes) {
        PetStatusHistory history = new PetStatusHistory(
                UUID.randomUUID(),
                petId,
                fromStatus,
                toStatus,
                changedBy,
                Instant.now(),
                notes
        );
        history.isNew = true;
        return history;
    }

    @Override
    public UUID getId() { return id; }
    public UUID getPetId() { return petId; }
    public PetStatus getFromStatus() { return fromStatus; }
    public PetStatus getToStatus() { return toStatus; }
    public UUID getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public String getNotes() { return notes; }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
