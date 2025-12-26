package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.AuditLog;
import com.example.foreverhome.moderation.domain.admin.ImpersonationSession;
import com.example.foreverhome.moderation.repository.admin.AuditLogRepository;
import com.example.foreverhome.moderation.repository.admin.ImpersonationSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminImpersonationService {

    private final ImpersonationSessionRepository impersonationSessionRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminImpersonationService(ImpersonationSessionRepository impersonationSessionRepository,
                                      AuditLogRepository auditLogRepository) {
        this.impersonationSessionRepository = impersonationSessionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public ImpersonationSession startImpersonation(UUID adminUserId, UUID targetUserId,
                                                    String ipAddress, String reason) {
        // Check if admin already has an active impersonation session
        Optional<ImpersonationSession> existing = impersonationSessionRepository.findActiveByAdminUserId(adminUserId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Admin already has an active impersonation session. Please end it first.");
        }

        ImpersonationSession session = ImpersonationSession.create(adminUserId, targetUserId, ipAddress, reason);
        ImpersonationSession saved = impersonationSessionRepository.save(session);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.IMPERSONATION_STARTED,
                "IMPERSONATION",
                saved.getId(),
                adminUserId,
                "Started impersonating user " + targetUserId + ". Reason: " + reason
        ));

        return saved;
    }

    @Transactional
    public void endImpersonation(String token, UUID adminUserId) {
        ImpersonationSession session = impersonationSessionRepository.findActiveByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("No active impersonation session found"));

        if (!session.getAdminUserId().equals(adminUserId)) {
            throw new IllegalArgumentException("Cannot end another admin's impersonation session");
        }

        session.end();
        impersonationSessionRepository.save(session);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.IMPERSONATION_ENDED,
                "IMPERSONATION",
                session.getId(),
                adminUserId,
                "Ended impersonation of user " + session.getTargetUserId()
        ));
    }

    @Transactional
    public void endImpersonationById(UUID sessionId, UUID adminUserId) {
        ImpersonationSession session = impersonationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Impersonation session not found"));

        session.end();
        impersonationSessionRepository.save(session);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.IMPERSONATION_ENDED,
                "IMPERSONATION",
                session.getId(),
                adminUserId,
                "Ended impersonation of user " + session.getTargetUserId()
        ));
    }

    public Optional<ImpersonationSession> validateToken(String token) {
        return impersonationSessionRepository.findActiveByToken(token);
    }

    public Optional<ImpersonationSession> getActiveSessionForAdmin(UUID adminUserId) {
        return impersonationSessionRepository.findActiveByAdminUserId(adminUserId);
    }

    public List<ImpersonationSession> getSessionHistory(int page, int size) {
        int offset = (page - 1) * size;
        return impersonationSessionRepository.findAllPaged(size, offset);
    }

    public List<ImpersonationSession> getSessionHistoryForAdmin(UUID adminUserId, int limit) {
        return impersonationSessionRepository.findByAdminUserId(adminUserId, limit);
    }

    public List<ImpersonationSession> getSessionHistoryForTarget(UUID targetUserId, int limit) {
        return impersonationSessionRepository.findByTargetUserId(targetUserId, limit);
    }

    public long countActiveSessions() {
        return impersonationSessionRepository.countActive();
    }

    public ImpersonationDashboard getDashboard() {
        long activeSessions = countActiveSessions();
        List<ImpersonationSession> recentSessions = getSessionHistory(1, 20);

        return new ImpersonationDashboard(activeSessions, recentSessions);
    }

    public record ImpersonationDashboard(
            long activeSessions,
            List<ImpersonationSession> recentSessions
    ) {}
}
