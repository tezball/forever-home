package com.example.foreverhome.domain.moderation;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Content flag representing a report about potentially inappropriate content.
 */
@Table("content_flags")
public class ContentFlag {

    @Id
    private UUID id;

    @Column("content_type")
    private ContentType contentType;

    @Column("content_id")
    private UUID contentId;

    @Column("reporter_id")
    private UUID reporterId;

    @Column("reason")
    private FlagReason reason;

    @Column("description")
    private String description;

    @Column("status")
    private FlagStatus status;

    @Column("reviewed_by")
    private UUID reviewedBy;

    @Column("reviewed_at")
    private Instant reviewedAt;

    @Column("resolution_notes")
    private String resolutionNotes;

    @Column("created_at")
    private Instant createdAt;

    protected ContentFlag() {
    }

    private ContentFlag(UUID id, ContentType contentType, UUID contentId, UUID reporterId,
                        FlagReason reason, String description, FlagStatus status,
                        UUID reviewedBy, Instant reviewedAt, String resolutionNotes, Instant createdAt) {
        this.id = id;
        this.contentType = contentType;
        this.contentId = contentId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.description = description;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.resolutionNotes = resolutionNotes;
        this.createdAt = createdAt;
    }

    public static ContentFlag create(ContentType contentType, UUID contentId, UUID reporterId,
                                     FlagReason reason, String description) {
        if (contentType == null) {
            throw new IllegalArgumentException("contentType cannot be null");
        }
        if (contentId == null) {
            throw new IllegalArgumentException("contentId cannot be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("reason cannot be null");
        }
        return new ContentFlag(UUID.randomUUID(), contentType, contentId, reporterId,
                reason, description, FlagStatus.PENDING, null, null, null, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public UUID getContentId() {
        return contentId;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public FlagReason getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public FlagStatus getStatus() {
        return status;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void approve(UUID reviewerId, String notes) {
        this.status = FlagStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.resolutionNotes = notes;
    }

    public void dismiss(UUID reviewerId, String notes) {
        this.status = FlagStatus.DISMISSED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.resolutionNotes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentFlag that = (ContentFlag) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum ContentType {
        PET,
        RESCUE_ORG,
        USER
    }

    public enum FlagReason {
        INAPPROPRIATE_CONTENT,
        SPAM,
        MISLEADING_INFO,
        FRAUD,
        ABUSE,
        OTHER
    }

    public enum FlagStatus {
        PENDING,
        APPROVED,
        DISMISSED
    }
}
