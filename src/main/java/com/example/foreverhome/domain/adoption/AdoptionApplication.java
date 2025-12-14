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
 * Formal request from an adopter to adopt a specific pet.
 */
@Table("adoption_applications")
public class AdoptionApplication implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("pet_id")
    private UUID petId;

    @Column("adopter_id")
    private UUID adopterId;

    @Column("status")
    private ApplicationStatus status;

    @Column("living_situation")
    private String livingSituation;

    @Column("pet_experience")
    private String petExperience;

    @Column("why_adopt")
    private String whyAdopt;

    @Column("submitted_at")
    private Instant submittedAt;

    @Column("reviewed_at")
    private Instant reviewedAt;

    @Column("reviewed_by")
    private UUID reviewedBy;

    @Column("rejection_reason")
    private String rejectionReason;

    @Transient
    private boolean isNew = false;

    protected AdoptionApplication() {
    }

    private AdoptionApplication(UUID id, UUID petId, UUID adopterId, ApplicationStatus status,
                                 String livingSituation, String petExperience, String whyAdopt,
                                 Instant submittedAt, Instant reviewedAt, UUID reviewedBy,
                                 String rejectionReason, boolean isNew) {
        this.id = id;
        this.petId = petId;
        this.adopterId = adopterId;
        this.status = status;
        this.livingSituation = livingSituation;
        this.petExperience = petExperience;
        this.whyAdopt = whyAdopt;
        this.submittedAt = submittedAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.rejectionReason = rejectionReason;
        this.isNew = isNew;
    }

    public static AdoptionApplication create(UUID petId, UUID adopterId, String message) {
        if (petId == null) {
            throw new IllegalArgumentException("petId cannot be null");
        }
        if (adopterId == null) {
            throw new IllegalArgumentException("adopterId cannot be null");
        }
        return new AdoptionApplication(UUID.randomUUID(), petId, adopterId, ApplicationStatus.SUBMITTED,
                null, null, message, Instant.now(), null, null, null, true);
    }

    public static AdoptionApplication create(UUID petId, UUID adopterId, String livingSituation,
                                              String petExperience, String whyAdopt) {
        if (petId == null) {
            throw new IllegalArgumentException("petId cannot be null");
        }
        if (adopterId == null) {
            throw new IllegalArgumentException("adopterId cannot be null");
        }
        if (livingSituation == null || livingSituation.isBlank()) {
            throw new IllegalArgumentException("livingSituation cannot be null or blank");
        }
        if (petExperience == null || petExperience.isBlank()) {
            throw new IllegalArgumentException("petExperience cannot be null or blank");
        }
        if (whyAdopt == null || whyAdopt.isBlank()) {
            throw new IllegalArgumentException("whyAdopt cannot be null or blank");
        }

        return new AdoptionApplication(UUID.randomUUID(), petId, adopterId, ApplicationStatus.SUBMITTED,
                livingSituation, petExperience, whyAdopt, Instant.now(), null, null, null, true);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getId() { return id; }
    public UUID getPetId() { return petId; }
    public UUID getAdopterId() { return adopterId; }
    public ApplicationStatus getStatus() { return status; }
    public String getLivingSituation() { return livingSituation; }
    public String getPetExperience() { return petExperience; }
    public String getWhyAdopt() { return whyAdopt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public String getRejectionReason() { return rejectionReason; }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean canTransitionTo(ApplicationStatus newStatus) {
        return status.canTransitionTo(newStatus);
    }

    public void approve() {
        if (!status.canTransitionTo(ApplicationStatus.APPROVED)) {
            throw new IllegalStateException("Cannot approve from status: " + status);
        }
        this.status = ApplicationStatus.APPROVED;
        this.reviewedAt = Instant.now();
    }

    public void reject() {
        if (!status.canTransitionTo(ApplicationStatus.REJECTED)) {
            throw new IllegalStateException("Cannot reject from status: " + status);
        }
        this.status = ApplicationStatus.REJECTED;
        this.reviewedAt = Instant.now();
    }

    public void markUnderReview(UUID reviewerId) {
        if (!status.canTransitionTo(ApplicationStatus.UNDER_REVIEW)) {
            throw new IllegalStateException("Cannot mark as under review from status: " + status);
        }
        this.status = ApplicationStatus.UNDER_REVIEW;
        this.reviewedBy = reviewerId;
    }

    public void approve(UUID reviewerId) {
        if (!status.canTransitionTo(ApplicationStatus.APPROVED)) {
            throw new IllegalStateException("Cannot approve from status: " + status);
        }
        this.status = ApplicationStatus.APPROVED;
        this.reviewedAt = Instant.now();
        this.reviewedBy = reviewerId;
    }

    public void reject(UUID reviewerId, String reason) {
        if (!status.canTransitionTo(ApplicationStatus.REJECTED)) {
            throw new IllegalStateException("Cannot reject from status: " + status);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("rejection reason is required");
        }
        this.status = ApplicationStatus.REJECTED;
        this.reviewedAt = Instant.now();
        this.reviewedBy = reviewerId;
        this.rejectionReason = reason;
    }

    public void withdraw() {
        if (!status.canWithdraw()) {
            throw new IllegalStateException("Cannot withdraw from status: " + status);
        }
        this.status = ApplicationStatus.WITHDRAWN;
    }

    /**
     * Marks the application as finalized after adoption completion.
     * Can only be called when status is APPROVED.
     */
    public void markAsFinalized() {
        if (!status.canTransitionTo(ApplicationStatus.FINALIZED)) {
            throw new IllegalStateException("Cannot finalize from status: " + status);
        }
        this.status = ApplicationStatus.FINALIZED;
        this.reviewedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdoptionApplication that = (AdoptionApplication) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
