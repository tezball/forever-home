package com.example.foreverhome.controller;

import com.example.foreverhome.config.RateLimitFilter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal API endpoints for admin service integration.
 * These endpoints are intended for use by the moderation-service only.
 */
@RestController
@RequestMapping("/api/internal")
public class InternalApiController {

    private final JdbcClient jdbcClient;
    private final RateLimitFilter rateLimitFilter;

    public InternalApiController(JdbcClient jdbcClient, RateLimitFilter rateLimitFilter) {
        this.jdbcClient = jdbcClient;
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * Get current rate limit status for all IPs.
     */
    @GetMapping("/rate-limits")
    public List<RateLimitFilter.RateLimitStatus> getRateLimits() {
        return rateLimitFilter.getRateLimitStatus();
    }

    /**
     * Check if an IP address is blocked.
     */
    @GetMapping("/blocked-ip/check")
    public BlockedIpCheckResponse checkBlockedIp(@RequestParam String ip) {
        boolean blocked = isIpBlocked(ip);
        return new BlockedIpCheckResponse(ip, blocked);
    }

    /**
     * Get list of all blocked IPs (for caching by rate limit filter).
     */
    @GetMapping("/blocked-ips")
    public List<BlockedIpEntry> getBlockedIps() {
        return jdbcClient.sql("""
            SELECT ip_address, cidr_range, permanent, expires_at
            FROM blocked_ips
            WHERE active = true
            AND (permanent = true OR expires_at > NOW())
            """)
                .query((rs, rowNum) -> new BlockedIpEntry(
                        rs.getString("ip_address"),
                        rs.getString("cidr_range"),
                        rs.getBoolean("permanent"),
                        rs.getTimestamp("expires_at") != null ?
                                rs.getTimestamp("expires_at").toInstant().toString() : null
                ))
                .list();
    }

    /**
     * Validate an impersonation token.
     */
    @GetMapping("/impersonation/validate")
    public ImpersonationValidation validateImpersonation(@RequestParam String token) {
        return jdbcClient.sql("""
            SELECT is.id, is.admin_user_id, is.target_user_id, u.email as target_email, u.role as target_role
            FROM impersonation_sessions is
            JOIN app_users u ON is.target_user_id = u.id
            WHERE is.token = :token
            AND is.ended_at IS NULL
            AND is.expires_at > NOW()
            """)
                .param("token", token)
                .query((rs, rowNum) -> new ImpersonationValidation(
                        true,
                        rs.getString("id"),
                        rs.getString("admin_user_id"),
                        rs.getString("target_user_id"),
                        rs.getString("target_email"),
                        rs.getString("target_role")
                ))
                .optional()
                .orElse(new ImpersonationValidation(false, null, null, null, null, null));
    }

    private boolean isIpBlocked(String ip) {
        // Check for exact IP match
        Long exactMatch = jdbcClient.sql("""
            SELECT COUNT(*) FROM blocked_ips
            WHERE ip_address = :ip
            AND active = true
            AND (permanent = true OR expires_at > NOW())
            """)
                .param("ip", ip)
                .query(Long.class)
                .single();

        if (exactMatch > 0) {
            return true;
        }

        // Check for CIDR match (simplified - would need proper CIDR matching for production)
        // For now, just check if IP starts with any blocked CIDR prefix
        Long cidrMatch = jdbcClient.sql("""
            SELECT COUNT(*) FROM blocked_ips
            WHERE cidr_range IS NOT NULL
            AND active = true
            AND (permanent = true OR expires_at > NOW())
            AND :ip LIKE REPLACE(SPLIT_PART(cidr_range, '/', 1), '.0', '.%')
            """)
                .param("ip", ip)
                .query(Long.class)
                .single();

        return cidrMatch > 0;
    }

    public record BlockedIpCheckResponse(String ip, boolean blocked) {}

    public record BlockedIpEntry(String ipAddress, String cidrRange, boolean permanent, String expiresAt) {}

    public record ImpersonationValidation(
            boolean valid,
            String sessionId,
            String adminUserId,
            String targetUserId,
            String targetEmail,
            String targetRole
    ) {}
}
