package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.AuditLog;
import com.example.foreverhome.moderation.domain.admin.BlockedIp;
import com.example.foreverhome.moderation.repository.admin.AuditLogRepository;
import com.example.foreverhome.moderation.repository.admin.BlockedIpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminBlockedIpService {

    private final BlockedIpRepository blockedIpRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminBlockedIpService(BlockedIpRepository blockedIpRepository,
                                  AuditLogRepository auditLogRepository) {
        this.blockedIpRepository = blockedIpRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public BlockedIpDashboard getDashboard() {
        List<BlockedIp> activeBlocks = blockedIpRepository.findAllActive();
        long permanentCount = blockedIpRepository.countPermanent();
        long temporaryCount = blockedIpRepository.countTemporary();

        return new BlockedIpDashboard(activeBlocks, permanentCount, temporaryCount);
    }

    public List<BlockedIp> getAllBlocked(int page, int size) {
        int offset = (page - 1) * size;
        return blockedIpRepository.findAllPaged(size, offset);
    }

    public Optional<BlockedIp> findById(UUID id) {
        return blockedIpRepository.findById(id);
    }

    @Transactional
    public BlockedIp blockIp(String ipAddress, String reason, UUID blockedBy, boolean permanent) {
        BlockedIp block = BlockedIp.blockIp(ipAddress, reason, blockedBy, permanent);
        BlockedIp saved = blockedIpRepository.save(block);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.IP_BLOCKED,
                "BLOCKED_IP",
                saved.getId(),
                blockedBy,
                "IP " + ipAddress + " blocked. Reason: " + reason + ". Permanent: " + permanent
        ));

        return saved;
    }

    @Transactional
    public BlockedIp blockIpTemporary(String ipAddress, String reason, UUID blockedBy, int durationMinutes) {
        Instant expiresAt = Instant.now().plus(durationMinutes, ChronoUnit.MINUTES);
        BlockedIp block = BlockedIp.blockIpTemporary(ipAddress, reason, blockedBy, expiresAt);
        BlockedIp saved = blockedIpRepository.save(block);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.IP_BLOCKED,
                "BLOCKED_IP",
                saved.getId(),
                blockedBy,
                "IP " + ipAddress + " blocked temporarily for " + durationMinutes + " minutes. Reason: " + reason
        ));

        return saved;
    }

    @Transactional
    public BlockedIp blockCidr(String cidrRange, String reason, UUID blockedBy, boolean permanent) {
        BlockedIp block = BlockedIp.blockCidr(cidrRange, reason, blockedBy, permanent);
        BlockedIp saved = blockedIpRepository.save(block);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.IP_BLOCKED,
                "BLOCKED_IP",
                saved.getId(),
                blockedBy,
                "CIDR range " + cidrRange + " blocked. Reason: " + reason + ". Permanent: " + permanent
        ));

        return saved;
    }

    @Transactional
    public void unblock(UUID id, UUID unblockedBy) {
        blockedIpRepository.findById(id).ifPresent(block -> {
            block.unblock();
            blockedIpRepository.save(block);

            auditLogRepository.save(AuditLog.create(
                    AuditLog.AuditAction.IP_UNBLOCKED,
                    "BLOCKED_IP",
                    id,
                    unblockedBy,
                    "IP/CIDR " + block.getDisplayAddress() + " unblocked"
            ));
        });
    }

    public boolean isIpBlocked(String ipAddress) {
        List<BlockedIp> matches = blockedIpRepository.findActiveMatchingIp(ipAddress);
        return matches.stream().anyMatch(BlockedIp::isEffective);
    }

    @Transactional
    public int cleanupExpired() {
        List<BlockedIp> expired = blockedIpRepository.findExpired();
        int count = 0;
        for (BlockedIp block : expired) {
            block.unblock();
            blockedIpRepository.save(block);
            count++;
        }
        return count;
    }

    public record BlockedIpDashboard(
            List<BlockedIp> activeBlocks,
            long permanentCount,
            long temporaryCount
    ) {
        public long totalActive() {
            return permanentCount + temporaryCount;
        }
    }
}
