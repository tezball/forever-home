package com.example.foreverhome.moderation.domain.admin;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Table("impersonation_sessions")
public class ImpersonationSession {

    private static final int DEFAULT_EXPIRY_MINUTES = 15;

    @Id
    private UUID id;
    private UUID adminUserId;
    private UUID targetUserId;
    private String token;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant endedAt;
    private String ipAddress;
    private String reason;

    public ImpersonationSession() {
    }

    public static ImpersonationSession create(UUID adminUserId, UUID targetUserId, String ipAddress, String reason) {
        ImpersonationSession session = new ImpersonationSession();
        session.adminUserId = adminUserId;
        session.targetUserId = targetUserId;
        session.token = UUID.randomUUID().toString();
        session.createdAt = Instant.now();
        session.expiresAt = Instant.now().plus(DEFAULT_EXPIRY_MINUTES, ChronoUnit.MINUTES);
        session.ipAddress = ipAddress;
        session.reason = reason;
        return session;
    }

    public void end() {
        this.endedAt = Instant.now();
    }

    public boolean isActive() {
        return endedAt == null && Instant.now().isBefore(expiresAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public long getRemainingMinutes() {
        if (isExpired() || endedAt != null) return 0;
        return ChronoUnit.MINUTES.between(Instant.now(), expiresAt);
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAdminUserId() { return adminUserId; }
    public void setAdminUserId(UUID adminUserId) { this.adminUserId = adminUserId; }

    public UUID getTargetUserId() { return targetUserId; }
    public void setTargetUserId(UUID targetUserId) { this.targetUserId = targetUserId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
