package com.example.foreverhome.moderation.domain.admin;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("platform_settings")
public class PlatformSetting {

    public enum SettingType {
        BOOLEAN, STRING, INTEGER, JSON, HTML
    }

    @Id
    private UUID id;
    private String settingKey;
    private String settingValue;
    private SettingType settingType;
    private String category;
    private String description;
    private UUID updatedBy;
    private Instant updatedAt;

    public PlatformSetting() {
        this.updatedAt = Instant.now();
    }

    public PlatformSetting(String settingKey, String settingValue, SettingType settingType, String category) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.settingType = settingType;
        this.category = category;
        this.updatedAt = Instant.now();
    }

    public void updateValue(String newValue, UUID updatedBy) {
        this.settingValue = newValue;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public String getValueAsString() {
        return settingValue;
    }

    public boolean getValueAsBoolean() {
        return Boolean.parseBoolean(settingValue);
    }

    public int getValueAsInteger() {
        return Integer.parseInt(settingValue);
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }

    public SettingType getSettingType() { return settingType; }
    public void setSettingType(SettingType settingType) { this.settingType = settingType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
