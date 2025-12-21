package com.example.foreverhome.moderation.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks a batch moderation job.
 */
@Table("moderation_jobs")
public class ModerationJob implements Persistable<UUID> {

    @Id
    private UUID id;

    private JobStatus status;

    @Column("total_pets")
    private int totalPets;

    @Column("processed_pets")
    private int processedPets;

    @Column("flagged_pets")
    private int flaggedPets;

    @Column("text_only")
    private boolean textOnly;

    @Column("images_only")
    private boolean imagesOnly;

    @Column("started_at")
    private Instant startedAt;

    @Column("completed_at")
    private Instant completedAt;

    @Column("error_message")
    private String errorMessage;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected ModerationJob() {
    }

    public static ModerationJob create(int totalPets, boolean textOnly, boolean imagesOnly) {
        ModerationJob job = new ModerationJob();
        job.id = UUID.randomUUID();
        job.status = JobStatus.PENDING;
        job.totalPets = totalPets;
        job.processedPets = 0;
        job.flaggedPets = 0;
        job.textOnly = textOnly;
        job.imagesOnly = imagesOnly;
        job.createdAt = Instant.now();
        job.isNew = true;
        return job;
    }

    public void start() {
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void recordProgress(boolean wasFlagged) {
        this.processedPets++;
        if (wasFlagged) {
            this.flaggedPets++;
        }
    }

    public void complete() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        this.status = JobStatus.CANCELLED;
        this.completedAt = Instant.now();
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

    public JobStatus getStatus() {
        return status;
    }

    public int getTotalPets() {
        return totalPets;
    }

    public int getProcessedPets() {
        return processedPets;
    }

    public int getFlaggedPets() {
        return flaggedPets;
    }

    public boolean isTextOnly() {
        return textOnly;
    }

    public boolean isImagesOnly() {
        return imagesOnly;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public double getProgressPercent() {
        if (totalPets == 0) return 0;
        return (processedPets * 100.0) / totalPets;
    }
}
