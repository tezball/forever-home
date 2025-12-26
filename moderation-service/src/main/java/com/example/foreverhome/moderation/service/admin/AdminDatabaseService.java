package com.example.foreverhome.moderation.service.admin;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDatabaseService {

    private final JdbcClient jdbcClient;

    public AdminDatabaseService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public DatabaseDashboard getDashboard() {
        List<TableStats> tableStats = getTableStats();
        ConnectionStats connectionStats = getConnectionStats();
        DatabaseSize databaseSize = getDatabaseSize();

        return new DatabaseDashboard(tableStats, connectionStats, databaseSize);
    }

    public List<TableStats> getTableStats() {
        return jdbcClient.sql("""
            SELECT
                schemaname,
                relname as table_name,
                n_live_tup as row_count,
                n_dead_tup as dead_tuples,
                last_vacuum,
                last_autovacuum,
                last_analyze,
                pg_size_pretty(pg_total_relation_size(relid)) as total_size
            FROM pg_stat_user_tables
            ORDER BY n_live_tup DESC
            """)
                .query((rs, rowNum) -> new TableStats(
                        rs.getString("schemaname"),
                        rs.getString("table_name"),
                        rs.getLong("row_count"),
                        rs.getLong("dead_tuples"),
                        rs.getTimestamp("last_vacuum") != null ?
                                rs.getTimestamp("last_vacuum").toInstant().toString() : null,
                        rs.getTimestamp("last_autovacuum") != null ?
                                rs.getTimestamp("last_autovacuum").toInstant().toString() : null,
                        rs.getString("total_size")
                ))
                .list();
    }

    public ConnectionStats getConnectionStats() {
        int maxConnections = jdbcClient.sql("SHOW max_connections")
                .query(Integer.class)
                .single();

        long activeConnections = jdbcClient.sql("""
            SELECT COUNT(*) FROM pg_stat_activity
            WHERE state = 'active'
            """)
                .query(Long.class)
                .single();

        long idleConnections = jdbcClient.sql("""
            SELECT COUNT(*) FROM pg_stat_activity
            WHERE state = 'idle'
            """)
                .query(Long.class)
                .single();

        long totalConnections = jdbcClient.sql("""
            SELECT COUNT(*) FROM pg_stat_activity
            """)
                .query(Long.class)
                .single();

        return new ConnectionStats(maxConnections, activeConnections, idleConnections, totalConnections);
    }

    public DatabaseSize getDatabaseSize() {
        String dbName = jdbcClient.sql("SELECT current_database()")
                .query(String.class)
                .single();

        String totalSize = jdbcClient.sql("SELECT pg_size_pretty(pg_database_size(current_database()))")
                .query(String.class)
                .single();

        long sizeBytes = jdbcClient.sql("SELECT pg_database_size(current_database())")
                .query(Long.class)
                .single();

        return new DatabaseSize(dbName, totalSize, sizeBytes);
    }

    public List<ActiveQuery> getActiveQueries() {
        return jdbcClient.sql("""
            SELECT
                pid,
                usename as username,
                application_name,
                client_addr,
                state,
                query,
                EXTRACT(EPOCH FROM (NOW() - query_start)) as duration_seconds
            FROM pg_stat_activity
            WHERE state != 'idle' AND query != ''
            ORDER BY query_start
            LIMIT 50
            """)
                .query((rs, rowNum) -> new ActiveQuery(
                        rs.getInt("pid"),
                        rs.getString("username"),
                        rs.getString("application_name"),
                        rs.getString("client_addr"),
                        rs.getString("state"),
                        rs.getString("query"),
                        rs.getDouble("duration_seconds")
                ))
                .list();
    }

    public List<SlowQuery> getSlowQueryCandidates() {
        // Get queries that might be slow based on sequential scans
        return jdbcClient.sql("""
            SELECT
                schemaname,
                relname as table_name,
                seq_scan,
                seq_tup_read,
                idx_scan,
                idx_tup_fetch,
                CASE WHEN seq_scan > 0
                     THEN seq_tup_read / seq_scan
                     ELSE 0
                END as avg_seq_tup_read
            FROM pg_stat_user_tables
            WHERE seq_scan > 0
            ORDER BY seq_tup_read DESC
            LIMIT 20
            """)
                .query((rs, rowNum) -> new SlowQuery(
                        rs.getString("schemaname"),
                        rs.getString("table_name"),
                        rs.getLong("seq_scan"),
                        rs.getLong("seq_tup_read"),
                        rs.getLong("idx_scan"),
                        rs.getLong("avg_seq_tup_read")
                ))
                .list();
    }

    public record DatabaseDashboard(
            List<TableStats> tableStats,
            ConnectionStats connectionStats,
            DatabaseSize databaseSize
    ) {}

    public record TableStats(
            String schema,
            String tableName,
            long rowCount,
            long deadTuples,
            String lastVacuum,
            String lastAutovacuum,
            String totalSize
    ) {}

    public record ConnectionStats(
            int maxConnections,
            long activeConnections,
            long idleConnections,
            long totalConnections
    ) {
        public double usagePercent() {
            return (double) totalConnections / maxConnections * 100;
        }
    }

    public record DatabaseSize(
            String name,
            String prettySize,
            long sizeBytes
    ) {}

    public record ActiveQuery(
            int pid,
            String username,
            String applicationName,
            String clientAddress,
            String state,
            String query,
            double durationSeconds
    ) {}

    public record SlowQuery(
            String schema,
            String tableName,
            long seqScans,
            long seqTuplesRead,
            long indexScans,
            long avgSeqTuplesRead
    ) {}
}
