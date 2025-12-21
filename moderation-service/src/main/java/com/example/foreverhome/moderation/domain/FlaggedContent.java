package com.example.foreverhome.moderation.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Details of a specific flag raised during content moderation.
 */
@Table("flagged_content")
public class FlaggedContent implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("moderation_result_id")
    private UUID moderationResultId;

    private ModerationCategory category;

    private Severity severity;

    private String description;

    @Column("suggested_action")
    private String suggestedAction;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected FlaggedContent() {
    }

    public static FlaggedContent create(UUID moderationResultId, ModerationCategory category,
                                         Severity severity, String description, String suggestedAction) {
        FlaggedContent flag = new FlaggedContent();
        flag.id = UUID.randomUUID();
        flag.moderationResultId = moderationResultId;
        flag.category = category;
        flag.severity = severity;
        flag.description = description;
        flag.suggestedAction = suggestedAction;
        flag.createdAt = Instant.now();
        flag.isNew = true;
        return flag;
    }

    public static FlaggedContent create(ModerationCategory category, Severity severity,
                                         String description, String suggestedAction) {
        return create(null, category, severity, description, suggestedAction);
    }

    public void setModerationResultId(UUID moderationResultId) {
        this.moderationResultId = moderationResultId;
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

    public UUID getModerationResultId() {
        return moderationResultId;
    }

    public ModerationCategory getCategory() {
        return category;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
