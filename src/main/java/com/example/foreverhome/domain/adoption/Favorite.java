package com.example.foreverhome.domain.adoption;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pets that adopters have saved for later.
 */
@Table("favorites")
public class Favorite implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("adopter_id")
    private UUID adopterId;

    @Column("pet_id")
    private UUID petId;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew = false;

    protected Favorite() {
    }

    private Favorite(UUID id, UUID adopterId, UUID petId, Instant createdAt, boolean isNew) {
        this.id = id;
        this.adopterId = adopterId;
        this.petId = petId;
        this.createdAt = createdAt;
        this.isNew = isNew;
    }

    public static Favorite create(UUID adopterId, UUID petId) {
        if (adopterId == null) {
            throw new IllegalArgumentException("adopterId cannot be null");
        }
        if (petId == null) {
            throw new IllegalArgumentException("petId cannot be null");
        }
        return new Favorite(UUID.randomUUID(), adopterId, petId, Instant.now(), true);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getId() { return id; }
    public UUID getAdopterId() { return adopterId; }
    public UUID getPetId() { return petId; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Favorite favorite = (Favorite) o;
        return Objects.equals(id, favorite.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
