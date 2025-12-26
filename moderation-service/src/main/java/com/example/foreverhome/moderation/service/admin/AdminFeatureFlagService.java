package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.AuditLog;
import com.example.foreverhome.moderation.domain.admin.FeatureFlag;
import com.example.foreverhome.moderation.repository.admin.AuditLogRepository;
import com.example.foreverhome.moderation.repository.admin.FeatureFlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminFeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminFeatureFlagService(FeatureFlagRepository featureFlagRepository,
                                    AuditLogRepository auditLogRepository) {
        this.featureFlagRepository = featureFlagRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<FeatureFlag> getAllFlags() {
        return featureFlagRepository.findAllOrderByFlagKey();
    }

    public List<FeatureFlag> getEnabledFlags() {
        return featureFlagRepository.findAllEnabled();
    }

    public Optional<FeatureFlag> findByKey(String flagKey) {
        return featureFlagRepository.findByFlagKey(flagKey);
    }

    public Optional<FeatureFlag> findById(UUID id) {
        return featureFlagRepository.findById(id);
    }

    public boolean isEnabled(String flagKey) {
        return featureFlagRepository.findByFlagKey(flagKey)
                .map(FeatureFlag::isEnabled)
                .orElse(false);
    }

    public boolean isEnabledForRole(String flagKey, String role) {
        return featureFlagRepository.findByFlagKey(flagKey)
                .map(flag -> flag.isEnabledForRole(role))
                .orElse(false);
    }

    @Transactional
    public FeatureFlag createFlag(String flagKey, String description, boolean enabled, UUID createdBy) {
        if (featureFlagRepository.existsByFlagKey(flagKey)) {
            throw new IllegalArgumentException("Feature flag with key '" + flagKey + "' already exists");
        }

        FeatureFlag flag = new FeatureFlag(flagKey, enabled, description);
        FeatureFlag saved = featureFlagRepository.save(flag);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.FEATURE_FLAG_CREATED,
                "FEATURE_FLAG",
                saved.getId(),
                createdBy,
                "Created feature flag: " + flagKey + " (enabled: " + enabled + ")"
        ));

        return saved;
    }

    @Transactional
    public FeatureFlag enableFlag(UUID flagId, UUID enabledBy) {
        FeatureFlag flag = featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found"));

        flag.enable();
        FeatureFlag saved = featureFlagRepository.save(flag);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.FEATURE_FLAG_TOGGLED,
                "FEATURE_FLAG",
                flagId,
                enabledBy,
                "Enabled feature flag: " + flag.getFlagKey()
        ));

        return saved;
    }

    @Transactional
    public FeatureFlag disableFlag(UUID flagId, UUID disabledBy) {
        FeatureFlag flag = featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found"));

        flag.disable();
        FeatureFlag saved = featureFlagRepository.save(flag);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.FEATURE_FLAG_TOGGLED,
                "FEATURE_FLAG",
                flagId,
                disabledBy,
                "Disabled feature flag: " + flag.getFlagKey()
        ));

        return saved;
    }

    @Transactional
    public FeatureFlag updateRollout(UUID flagId, int percentage, UUID updatedBy) {
        FeatureFlag flag = featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found"));

        flag.setRolloutPercentage(percentage);
        FeatureFlag saved = featureFlagRepository.save(flag);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.FEATURE_FLAG_UPDATED,
                "FEATURE_FLAG",
                flagId,
                updatedBy,
                "Updated rollout percentage for " + flag.getFlagKey() + " to " + percentage + "%"
        ));

        return saved;
    }

    @Transactional
    public FeatureFlag updateTargetRoles(UUID flagId, List<String> roles, UUID updatedBy) {
        FeatureFlag flag = featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found"));

        flag.setTargetRoles(roles);
        FeatureFlag saved = featureFlagRepository.save(flag);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.FEATURE_FLAG_UPDATED,
                "FEATURE_FLAG",
                flagId,
                updatedBy,
                "Updated target roles for " + flag.getFlagKey() + " to: " + roles
        ));

        return saved;
    }

    @Transactional
    public void deleteFlag(UUID flagId, UUID deletedBy) {
        FeatureFlag flag = featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found"));

        String flagKey = flag.getFlagKey();
        featureFlagRepository.delete(flag);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.FEATURE_FLAG_DELETED,
                "FEATURE_FLAG",
                flagId,
                deletedBy,
                "Deleted feature flag: " + flagKey
        ));
    }

    public FeatureFlagSummary getSummary() {
        List<FeatureFlag> allFlags = getAllFlags();
        long enabled = allFlags.stream().filter(FeatureFlag::isEnabled).count();
        long disabled = allFlags.size() - enabled;

        return new FeatureFlagSummary(allFlags.size(), enabled, disabled);
    }

    public record FeatureFlagSummary(
            long total,
            long enabled,
            long disabled
    ) {}
}
