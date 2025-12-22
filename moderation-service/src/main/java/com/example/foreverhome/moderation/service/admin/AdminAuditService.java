package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.AuditLog;
import com.example.foreverhome.moderation.repository.admin.AuditLogRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminAuditService {

    private final AuditLogRepository auditLogRepository;
    private final JdbcClient jdbcClient;

    public AdminAuditService(AuditLogRepository auditLogRepository, JdbcClient jdbcClient) {
        this.auditLogRepository = auditLogRepository;
        this.jdbcClient = jdbcClient;
    }

    public List<AuditLog> getAuditLogs(int page, int size) {
        int limit = Math.min(size, 100);
        int offset = (page - 1) * limit;
        return auditLogRepository.findAllPaged(limit, offset);
    }

    public long countAuditLogs() {
        return auditLogRepository.count();
    }

    @Transactional
    public int resetModerationStatuses() {
        int count = jdbcClient.sql("UPDATE pets SET moderation_status = 'PENDING'")
                .update();

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.MODERATION_RESET,
                "PET",
                null,
                null,
                "Reset all pet moderation statuses to PENDING. " + count + " pets affected"
        ));

        return count;
    }
}
