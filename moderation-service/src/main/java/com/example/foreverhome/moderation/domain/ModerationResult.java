package com.example.foreverhome.moderation.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Result of moderating a piece of content (text field or image).
 */
@Table("moderation_results")
public class ModerationResult implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("pet_id")
    private UUID petId;

    @Column("content_type")
    private ContentType contentType;

    @Column("content_identifier")
    private String contentIdentifier;

    private ModerationStatus status;

    @Column("confidence_score")
    private Double confidenceScore;

    @Column("model_used")
    private String modelUsed;

    @Column("raw_response")
    private String rawResponse;

    @Column("created_at")
    private Instant createdAt;

    @Column("reviewed_at")
    private Instant reviewedAt;

    @Column("reviewed_by")
    private UUID reviewedBy;

    @Column("review_notes")
    private String reviewNotes;

    @Transient
    private boolean isNew = true;

    @Transient
    private List<FlaggedContent> flags = new ArrayList<>();

    protected ModerationResult() {
    }

    public static ModerationResult create(UUID petId, ContentType contentType, String contentIdentifier,
                                          ModerationStatus status, Double confidenceScore,
                                          String modelUsed, String rawResponse) {
        ModerationResult result = new ModerationResult();
        result.id = UUID.randomUUID();
        result.petId = petId;
        result.contentType = contentType;
        result.contentIdentifier = contentIdentifier;
        result.status = status;
        result.confidenceScore = confidenceScore;
        result.modelUsed = modelUsed;
        result.rawResponse = rawResponse;
        result.createdAt = Instant.now();
        result.isNew = true;
        return result;
    }

    public static ModerationResult createApproved(UUID petId, ContentType contentType, String contentIdentifier,
                                                   String modelUsed, Double confidenceScore) {
        return create(petId, contentType, contentIdentifier, ModerationStatus.APPROVED,
                confidenceScore, modelUsed, null);
    }

    public static ModerationResult createFlagged(UUID petId, ContentType contentType, String contentIdentifier,
                                                  String modelUsed, Double confidenceScore, String rawResponse) {
        return create(petId, contentType, contentIdentifier, ModerationStatus.FLAGGED,
                confidenceScore, modelUsed, rawResponse);
    }

    public static ModerationResult createPending(UUID petId, ContentType contentType, String contentIdentifier,
                                                  String modelUsed, String errorMessage) {
        return create(petId, contentType, contentIdentifier, ModerationStatus.PENDING,
                null, modelUsed, "Error: " + errorMessage);
    }

    public void markReviewed(UUID reviewedBy, ModerationStatus newStatus, String notes) {
        this.reviewedAt = Instant.now();
        this.reviewedBy = reviewedBy;
        this.reviewNotes = notes;
        this.status = newStatus;
    }

    public void addFlag(FlaggedContent flag) {
        this.flags.add(flag);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public UUID getPetId() {
        return petId;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getContentIdentifier() {
        return contentIdentifier;
    }

    public ModerationStatus getStatus() {
        return status;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public List<FlaggedContent> getFlags() {
        return flags;
    }

    public void setFlags(List<FlaggedContent> flags) {
        this.flags = flags;
    }
}
