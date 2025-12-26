package com.example.foreverhome.moderation.domain.admin;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table("feature_flags")
public class FeatureFlag {

    @Id
    private UUID id;
    private String flagKey;
    private boolean enabled;
    private String description;
    private Integer rolloutPercentage;
    private List<String> targetRoles;
    private Instant createdAt;
    private Instant updatedAt;

    public FeatureFlag() {
    }

    public FeatureFlag(String flagKey, boolean enabled, String description) {
        this.flagKey = flagKey;
        this.enabled = enabled;
        this.description = description;
        this.rolloutPercentage = 100;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void enable() {
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = Instant.now();
    }

    public void setRolloutPercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Rollout percentage must be between 0 and 100");
        }
        this.rolloutPercentage = percentage;
        this.updatedAt = Instant.now();
    }

    public void setTargetRoles(List<String> roles) {
        this.targetRoles = roles;
        this.updatedAt = Instant.now();
    }

    public boolean isEnabledForRole(String role) {
        if (!enabled) return false;
        if (targetRoles == null || targetRoles.isEmpty()) return true;
        return targetRoles.contains(role);
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getRolloutPercentage() { return rolloutPercentage; }

    public List<String> getTargetRoles() { return targetRoles; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
