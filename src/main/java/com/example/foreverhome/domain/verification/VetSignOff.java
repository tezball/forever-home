package com.example.foreverhome.domain.verification;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record of veterinary verification.
 * Once created, cannot be modified.
 */
@Table("vet_signoffs")
public class VetSignOff {

    @Id
    private UUID id;

    @Column("pet_id")
    private UUID petId;

    @Column("vet_id")
    private UUID vetId;

    @Column("neutered_date")
    private LocalDate neuteredDate;

    @Column("health_status")
    private HealthStatus healthStatus;

    @Column("health_notes")
    private String healthNotes;

    @Column("signed_off_at")
    private Instant signedOffAt;

    protected VetSignOff() {
    }

    private VetSignOff(UUID id, UUID petId, UUID vetId, LocalDate neuteredDate,
                       HealthStatus healthStatus, String healthNotes, Instant signedOffAt) {
        this.id = id;
        this.petId = petId;
        this.vetId = vetId;
        this.neuteredDate = neuteredDate;
        this.healthStatus = healthStatus;
        this.healthNotes = healthNotes;
        this.signedOffAt = signedOffAt;
    }

    public static VetSignOff create(UUID petId, UUID vetId, LocalDate neuteredDate,
                                     HealthStatus healthStatus, String healthNotes) {
        if (petId == null) {
            throw new IllegalArgumentException("petId cannot be null");
        }
        if (vetId == null) {
            throw new IllegalArgumentException("vetId cannot be null");
        }
        if (neuteredDate == null) {
            throw new IllegalArgumentException("neuteredDate cannot be null");
        }
        if (healthStatus == null) {
            throw new IllegalArgumentException("healthStatus cannot be null");
        }
        if (healthStatus.requiresHealthNotes() && (healthNotes == null || healthNotes.isBlank())) {
            throw new IllegalArgumentException("healthNotes required for status: " + healthStatus);
        }

        return new VetSignOff(UUID.randomUUID(), petId, vetId, neuteredDate,
                healthStatus, healthNotes, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getPetId() {
        return petId;
    }

    public UUID getVetId() {
        return vetId;
    }

    public LocalDate getNeuteredDate() {
        return neuteredDate;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public String getHealthNotes() {
        return healthNotes;
    }

    public Instant getSignedOffAt() {
        return signedOffAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VetSignOff that = (VetSignOff) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
