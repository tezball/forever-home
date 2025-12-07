package com.example.foreverhome.domain.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh token for JWT authentication.
 */
@Table("refresh_tokens")
public class RefreshToken implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("token")
    private String token;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("revoked")
    private boolean revoked;

    @Transient
    private boolean isNew = false;

    protected RefreshToken() {
    }

    private RefreshToken(UUID id, UUID userId, String token, Instant expiresAt, Instant createdAt, boolean revoked) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revoked = revoked;
        this.isNew = true;
    }

    public static RefreshToken create(UUID userId, String token, Instant expiresAt) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token cannot be null or blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt cannot be null");
        }
        return new RefreshToken(UUID.randomUUID(), userId, token, expiresAt, Instant.now(), false);
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void revoke() {
        this.revoked = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
