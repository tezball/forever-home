package com.example.foreverhome.moderation.service.admin;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AdminSessionService {

    private final JdbcClient jdbcClient;

    public AdminSessionService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public SessionDashboard getDashboard() {
        long activeSessions = countActiveSessions();
        List<UserSession> recentSessions = getRecentSessions(50);

        return new SessionDashboard(activeSessions, recentSessions);
    }

    public long countActiveSessions() {
        return jdbcClient.sql("""
            SELECT COUNT(*) FROM refresh_tokens
            WHERE revoked = false AND expires_at > NOW()
            """)
                .query(Long.class)
                .single();
    }

    public List<UserSession> getRecentSessions(int limit) {
        return jdbcClient.sql("""
            SELECT rt.id, rt.user_id, u.email, u.name, u.role,
                   rt.created_at, rt.expires_at, rt.revoked
            FROM refresh_tokens rt
            JOIN app_users u ON rt.user_id = u.id
            ORDER BY rt.created_at DESC
            LIMIT :limit
            """)
                .param("limit", limit)
                .query((rs, rowNum) -> new UserSession(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getBoolean("revoked"),
                        rs.getTimestamp("expires_at").toInstant().isAfter(Instant.now()) && !rs.getBoolean("revoked")
                ))
                .list();
    }

    public List<UserSession> getSessionsForUser(UUID userId) {
        return jdbcClient.sql("""
            SELECT rt.id, rt.user_id, u.email, u.name, u.role,
                   rt.created_at, rt.expires_at, rt.revoked
            FROM refresh_tokens rt
            JOIN app_users u ON rt.user_id = u.id
            WHERE rt.user_id = :userId
            ORDER BY rt.created_at DESC
            """)
                .param("userId", userId)
                .query((rs, rowNum) -> new UserSession(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getBoolean("revoked"),
                        rs.getTimestamp("expires_at").toInstant().isAfter(Instant.now()) && !rs.getBoolean("revoked")
                ))
                .list();
    }

    @Transactional
    public int revokeSession(UUID sessionId) {
        return jdbcClient.sql("UPDATE refresh_tokens SET revoked = true WHERE id = :sessionId")
                .param("sessionId", sessionId)
                .update();
    }

    @Transactional
    public int revokeAllSessionsForUser(UUID userId) {
        return jdbcClient.sql("UPDATE refresh_tokens SET revoked = true WHERE user_id = :userId")
                .param("userId", userId)
                .update();
    }

    @Transactional
    public int revokeAllSessions() {
        return jdbcClient.sql("UPDATE refresh_tokens SET revoked = true WHERE revoked = false")
                .update();
    }

    @Transactional
    public int cleanupExpiredSessions() {
        return jdbcClient.sql("DELETE FROM refresh_tokens WHERE expires_at < NOW()")
                .update();
    }

    public record SessionDashboard(
            long activeSessions,
            List<UserSession> recentSessions
    ) {}

    public record UserSession(
            UUID id,
            UUID userId,
            String email,
            String name,
            String role,
            Instant createdAt,
            Instant expiresAt,
            boolean revoked,
            boolean active
    ) {
        public String status() {
            if (revoked) return "Revoked";
            if (expiresAt.isBefore(Instant.now())) return "Expired";
            return "Active";
        }
    }
}
