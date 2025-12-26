package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.AuditLog;
import com.example.foreverhome.moderation.domain.admin.PlatformSetting;
import com.example.foreverhome.moderation.repository.admin.AuditLogRepository;
import com.example.foreverhome.moderation.repository.admin.PlatformSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AdminContentService {

    private final PlatformSettingRepository platformSettingRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminContentService(PlatformSettingRepository platformSettingRepository,
                                AuditLogRepository auditLogRepository) {
        this.platformSettingRepository = platformSettingRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<PlatformSetting> getAllSettings() {
        return platformSettingRepository.findAllOrderByCategoryAndKey();
    }

    public Map<String, List<PlatformSetting>> getSettingsByCategory() {
        List<PlatformSetting> allSettings = getAllSettings();
        Map<String, List<PlatformSetting>> byCategory = new LinkedHashMap<>();

        for (PlatformSetting setting : allSettings) {
            byCategory.computeIfAbsent(setting.getCategory(), k -> new ArrayList<>()).add(setting);
        }

        return byCategory;
    }

    public List<String> getAllCategories() {
        return platformSettingRepository.findAllCategories();
    }

    public List<PlatformSetting> getSettingsForCategory(String category) {
        return platformSettingRepository.findByCategory(category);
    }

    public Optional<PlatformSetting> getSetting(String settingKey) {
        return platformSettingRepository.findBySettingKey(settingKey);
    }

    public String getSettingValue(String settingKey, String defaultValue) {
        return platformSettingRepository.findBySettingKey(settingKey)
                .map(PlatformSetting::getValueAsString)
                .orElse(defaultValue);
    }

    public boolean getSettingAsBoolean(String settingKey, boolean defaultValue) {
        return platformSettingRepository.findBySettingKey(settingKey)
                .map(PlatformSetting::getValueAsBoolean)
                .orElse(defaultValue);
    }

    public int getSettingAsInteger(String settingKey, int defaultValue) {
        return platformSettingRepository.findBySettingKey(settingKey)
                .map(PlatformSetting::getValueAsInteger)
                .orElse(defaultValue);
    }

    @Transactional
    public PlatformSetting updateSetting(String settingKey, String newValue, UUID updatedBy) {
        PlatformSetting setting = platformSettingRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + settingKey));

        String oldValue = setting.getSettingValue();
        setting.updateValue(newValue, updatedBy);
        PlatformSetting saved = platformSettingRepository.save(setting);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.PLATFORM_SETTING_UPDATED,
                "PLATFORM_SETTING",
                saved.getId(),
                updatedBy,
                "Updated setting '" + settingKey + "' from '" + truncate(oldValue, 50) +
                        "' to '" + truncate(newValue, 50) + "'"
        ));

        return saved;
    }

    @Transactional
    public PlatformSetting createSetting(String settingKey, String settingValue,
                                          PlatformSetting.SettingType settingType,
                                          String category, String description,
                                          UUID createdBy) {
        if (platformSettingRepository.existsBySettingKey(settingKey)) {
            throw new IllegalArgumentException("Setting already exists: " + settingKey);
        }

        PlatformSetting setting = new PlatformSetting(settingKey, settingValue, settingType, category);
        setting.setDescription(description);
        setting.setUpdatedBy(createdBy);
        PlatformSetting saved = platformSettingRepository.save(setting);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.PLATFORM_SETTING_UPDATED,
                "PLATFORM_SETTING",
                saved.getId(),
                createdBy,
                "Created new setting: " + settingKey
        ));

        return saved;
    }

    @Transactional
    public void deleteSetting(String settingKey, UUID deletedBy) {
        PlatformSetting setting = platformSettingRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + settingKey));

        platformSettingRepository.delete(setting);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.PLATFORM_SETTING_UPDATED,
                "PLATFORM_SETTING",
                setting.getId(),
                deletedBy,
                "Deleted setting: " + settingKey
        ));
    }

    public ContentDashboard getDashboard() {
        Map<String, List<PlatformSetting>> byCategory = getSettingsByCategory();
        long totalSettings = platformSettingRepository.count();

        return new ContentDashboard(byCategory, totalSettings);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }

    public record ContentDashboard(
            Map<String, List<PlatformSetting>> settingsByCategory,
            long totalSettings
    ) {}
}
