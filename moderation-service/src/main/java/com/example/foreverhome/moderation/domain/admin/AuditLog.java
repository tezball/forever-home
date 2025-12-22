package com.example.foreverhome.moderation.domain.admin;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log for tracking administrative actions.
 */
@Table("audit_logs")
public class AuditLog implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = false;

    @Column("action")
    private AuditAction action;

    @Column("entity_type")
    private String entityType;

    @Column("entity_id")
    private UUID entityId;

    @Column("actor_id")
    private UUID actorId;

    @Column("details")
    private String details;

    @Column("ip_address")
    private String ipAddress;

    @Column("created_at")
    private Instant createdAt;

    protected AuditLog() {
    }

    private AuditLog(UUID id, AuditAction action, String entityType, UUID entityId,
                     UUID actorId, String details, String ipAddress, Instant createdAt) {
        this.id = id;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorId = actorId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.isNew = true;
    }

    public static AuditLog create(AuditAction action, String entityType, UUID entityId,
                                  UUID actorId, String details) {
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        return new AuditLog(UUID.randomUUID(), action, entityType, entityId,
                actorId, details, null, Instant.now());
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getDetails() {
        return details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum AuditAction {
        USER_CREATED,
        USER_ACTIVATED,
        USER_SUSPENDED,
        USER_REACTIVATED,
        USER_PASSWORD_RESET,
        USER_DELETED,
        RESCUE_ORG_APPROVED,
        RESCUE_ORG_REJECTED,
        CONTENT_FLAG_APPROVED,
        CONTENT_FLAG_DISMISSED,
        PET_REMOVED,
        MODERATION_RESET,
        SETTINGS_CHANGED
    }
}
