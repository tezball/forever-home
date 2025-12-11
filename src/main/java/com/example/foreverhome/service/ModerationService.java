package com.example.foreverhome.service;

import com.example.foreverhome.domain.moderation.AuditLog;
import com.example.foreverhome.domain.moderation.AuditLog.AuditAction;
import com.example.foreverhome.domain.moderation.ContentFlag;
import com.example.foreverhome.domain.moderation.ContentFlag.ContentType;
import com.example.foreverhome.domain.moderation.ContentFlag.FlagReason;
import com.example.foreverhome.domain.moderation.ContentFlag.FlagStatus;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.logging.UserJourneyLogger;
import com.example.foreverhome.repository.AuditLogRepository;
import com.example.foreverhome.repository.ContentFlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ModerationService {

    private final ContentFlagRepository contentFlagRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserJourneyLogger journeyLogger;

    public ModerationService(ContentFlagRepository contentFlagRepository,
                            AuditLogRepository auditLogRepository,
                            UserJourneyLogger journeyLogger) {
        this.contentFlagRepository = contentFlagRepository;
        this.auditLogRepository = auditLogRepository;
        this.journeyLogger = journeyLogger;
    }

    /**
     * Create a new content flag.
     */
    public ContentFlag createFlag(ContentType contentType, UUID contentId, UUID reporterId,
                                  FlagReason reason, String description) {
        ContentFlag flag = ContentFlag.create(contentType, contentId, reporterId, reason, description);
        ContentFlag saved = contentFlagRepository.save(flag);

        journeyLogger.logModeration(UserJourneyLogger.ACTION_FLAG_CREATE, saved.getId(),
                contentType.name(), contentId, reason.name() + ": " + description);

        return saved;
    }

    /**
     * Get all pending content flags.
     */
    @Transactional(readOnly = true)
    public List<ContentFlag> getPendingFlags() {
        return contentFlagRepository.findPendingFlags();
    }

    /**
     * Get flags with pagination.
     */
    @Transactional(readOnly = true)
    public List<ContentFlag> getFlags(FlagStatus status, int page, int size) {
        int offset = (page - 1) * size;
        if (status != null) {
            return contentFlagRepository.findByStatusPaged(status, size, offset);
        }
        return contentFlagRepository.findAllPaged(size, offset);
    }

    /**
     * Count flags by status.
     */
    @Transactional(readOnly = true)
    public long countFlags(FlagStatus status) {
        if (status != null) {
            return contentFlagRepository.countByStatus(status);
        }
        return contentFlagRepository.count();
    }

    /**
     * Approve a content flag (take action on the content).
     */
    public ContentFlag approveFlag(UUID flagId, UUID reviewerId, String notes) {
        ContentFlag flag = contentFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentFlag", flagId));

        flag.approve(reviewerId, notes);
        ContentFlag saved = contentFlagRepository.save(flag);

        // Log the action
        logAuditAction(AuditAction.CONTENT_FLAG_APPROVED, "ContentFlag", flagId, reviewerId,
                "Approved flag for " + flag.getContentType() + " " + flag.getContentId() + ": " + notes, null);

        journeyLogger.logModeration(UserJourneyLogger.ACTION_FLAG_APPROVE, flagId,
                flag.getContentType().name(), flag.getContentId(), notes);

        return saved;
    }

    /**
     * Dismiss a content flag (no action needed).
     */
    public ContentFlag dismissFlag(UUID flagId, UUID reviewerId, String notes) {
        ContentFlag flag = contentFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentFlag", flagId));

        flag.dismiss(reviewerId, notes);
        ContentFlag saved = contentFlagRepository.save(flag);

        // Log the action
        logAuditAction(AuditAction.CONTENT_FLAG_DISMISSED, "ContentFlag", flagId, reviewerId,
                "Dismissed flag for " + flag.getContentType() + " " + flag.getContentId() + ": " + notes, null);

        journeyLogger.logModeration(UserJourneyLogger.ACTION_FLAG_DISMISS, flagId,
                flag.getContentType().name(), flag.getContentId(), notes);

        return saved;
    }

    /**
     * Log an audit action.
     */
    public void logAuditAction(AuditAction action, String entityType, UUID entityId,
                               UUID actorId, String details, String ipAddress) {
        AuditLog log = AuditLog.create(action, entityType, entityId, actorId, details, ipAddress);
        auditLogRepository.save(log);
    }

    /**
     * Get recent audit logs.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs(int page, int size) {
        int offset = (page - 1) * size;
        return auditLogRepository.findAllPaged(size, offset);
    }

    /**
     * Get audit logs for a specific entity.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsForEntity(String entityType, UUID entityId) {
        return auditLogRepository.findByEntity(entityType, entityId);
    }
}
