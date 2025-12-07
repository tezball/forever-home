package com.example.foreverhome.domain.adoption;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Record of a completed adoption - the successful outcome.
 */
@Table("adoptions")
public class Adoption {

    @Id
    private UUID id;

    @Column("pet_id")
    private UUID petId;

    @Column("foster_id")
    private UUID fosterId;

    @Column("adopter_id")
    private UUID adopterId;

    @Column("rescue_org_id")
    private UUID rescueOrgId;

    @Column("vet_id")
    private UUID vetId;

    @Column("application_id")
    private UUID applicationId;

    @Column("adopted_at")
    private Instant adoptedAt;

    protected Adoption() {
    }

    private Adoption(UUID id, UUID petId, UUID fosterId, UUID adopterId, UUID rescueOrgId,
                     UUID vetId, UUID applicationId, Instant adoptedAt) {
        this.id = id;
        this.petId = petId;
        this.fosterId = fosterId;
        this.adopterId = adopterId;
        this.rescueOrgId = rescueOrgId;
        this.vetId = vetId;
        this.applicationId = applicationId;
        this.adoptedAt = adoptedAt;
    }

    public static Adoption create(UUID petId, UUID adopterId, UUID fosterId, UUID rescueOrgId) {
        if (petId == null) throw new IllegalArgumentException("petId cannot be null");
        if (adopterId == null) throw new IllegalArgumentException("adopterId cannot be null");
        if (fosterId == null) throw new IllegalArgumentException("fosterId cannot be null");
        if (rescueOrgId == null) throw new IllegalArgumentException("rescueOrgId cannot be null");

        return new Adoption(UUID.randomUUID(), petId, fosterId, adopterId, rescueOrgId,
                null, null, Instant.now());
    }

    public static Adoption create(UUID petId, UUID fosterId, UUID adopterId, UUID rescueOrgId,
                                   UUID vetId, UUID applicationId) {
        if (petId == null) throw new IllegalArgumentException("petId cannot be null");
        if (fosterId == null) throw new IllegalArgumentException("fosterId cannot be null");
        if (adopterId == null) throw new IllegalArgumentException("adopterId cannot be null");
        if (rescueOrgId == null) throw new IllegalArgumentException("rescueOrgId cannot be null");
        if (vetId == null) throw new IllegalArgumentException("vetId cannot be null");
        if (applicationId == null) throw new IllegalArgumentException("applicationId cannot be null");

        return new Adoption(UUID.randomUUID(), petId, fosterId, adopterId, rescueOrgId,
                vetId, applicationId, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getPetId() { return petId; }
    public UUID getFosterId() { return fosterId; }
    public UUID getAdopterId() { return adopterId; }
    public UUID getRescueOrgId() { return rescueOrgId; }
    public UUID getVetId() { return vetId; }
    public UUID getApplicationId() { return applicationId; }
    public Instant getAdoptedAt() { return adoptedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Adoption adoption = (Adoption) o;
        return Objects.equals(id, adoption.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
