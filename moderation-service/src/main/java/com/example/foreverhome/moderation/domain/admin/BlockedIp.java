package com.example.foreverhome.moderation.domain.admin;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("blocked_ips")
public class BlockedIp {

    @Id
    private UUID id;
    private String ipAddress;
    private String cidrRange;
    private String reason;
    private UUID blockedBy;
    private Instant blockedAt;
    private Instant expiresAt;
    private boolean permanent;
    private boolean active;

    public BlockedIp() {
        this.blockedAt = Instant.now();
        this.active = true;
    }

    public static BlockedIp blockIp(String ipAddress, String reason, UUID blockedBy, boolean permanent) {
        BlockedIp block = new BlockedIp();
        block.ipAddress = ipAddress;
        block.reason = reason;
        block.blockedBy = blockedBy;
        block.permanent = permanent;
        block.active = true;
        return block;
    }

    public static BlockedIp blockIpTemporary(String ipAddress, String reason, UUID blockedBy, Instant expiresAt) {
        BlockedIp block = blockIp(ipAddress, reason, blockedBy, false);
        block.expiresAt = expiresAt;
        return block;
    }

    public static BlockedIp blockCidr(String cidrRange, String reason, UUID blockedBy, boolean permanent) {
        BlockedIp block = new BlockedIp();
        block.cidrRange = cidrRange;
        block.reason = reason;
        block.blockedBy = blockedBy;
        block.permanent = permanent;
        block.active = true;
        return block;
    }

    public void unblock() {
        this.active = false;
    }

    public boolean isExpired() {
        if (permanent) return false;
        if (expiresAt == null) return false;
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isEffective() {
        return active && !isExpired();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getCidrRange() { return cidrRange; }
    public void setCidrRange(String cidrRange) { this.cidrRange = cidrRange; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public UUID getBlockedBy() { return blockedBy; }
    public void setBlockedBy(UUID blockedBy) { this.blockedBy = blockedBy; }

    public Instant getBlockedAt() { return blockedAt; }
    public void setBlockedAt(Instant blockedAt) { this.blockedAt = blockedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isPermanent() { return permanent; }
    public void setPermanent(boolean permanent) { this.permanent = permanent; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDisplayAddress() {
        return ipAddress != null ? ipAddress : cidrRange;
    }
}
