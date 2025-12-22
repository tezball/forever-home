package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.AuditLog;
import com.example.foreverhome.moderation.domain.admin.ContentFlag;
import com.example.foreverhome.moderation.domain.admin.ContentFlag.FlagStatus;
import com.example.foreverhome.moderation.repository.admin.AuditLogRepository;
import com.example.foreverhome.moderation.repository.admin.ContentFlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminFlagService {

    private final ContentFlagRepository flagRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminFlagService(ContentFlagRepository flagRepository, AuditLogRepository auditLogRepository) {
        this.flagRepository = flagRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public FlagSearchResult getFlags(FlagStatus status, int page, int size) {
        int limit = Math.min(size, 100);
        int offset = (page - 1) * limit;

        List<ContentFlag> flags;
        long totalCount;

        if (status != null) {
            flags = flagRepository.findByStatusPaged(status, limit, offset);
            totalCount = flagRepository.countByStatus(status);
        } else {
            flags = flagRepository.findAllPaged(limit, offset);
            totalCount = flagRepository.count();
        }

        int totalPages = (int) Math.ceil((double) totalCount / limit);

        return new FlagSearchResult(flags, page, limit, totalCount, totalPages);
    }

    public Optional<ContentFlag> getFlag(UUID flagId) {
        return flagRepository.findById(flagId);
    }

    public long countPending() {
        return flagRepository.countPending();
    }

    @Transactional
    public ContentFlag approveFlag(UUID flagId, String notes) {
        ContentFlag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + flagId));

        flag.approve(null, notes);
        flagRepository.save(flag);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.CONTENT_FLAG_APPROVED,
                "FLAG",
                flagId,
                null,
                "Approved content flag for " + flag.getContentType() + ": " + flag.getContentId()
        ));

        return flag;
    }

    @Transactional
    public ContentFlag dismissFlag(UUID flagId, String notes) {
        ContentFlag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + flagId));

        flag.dismiss(null, notes);
        flagRepository.save(flag);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.CONTENT_FLAG_DISMISSED,
                "FLAG",
                flagId,
                null,
                "Dismissed content flag for " + flag.getContentType() + ": " + flag.getContentId()
        ));

        return flag;
    }

    public record FlagSearchResult(
            List<ContentFlag> flags,
            int page,
            int size,
            long totalCount,
            int totalPages
    ) {}
}
