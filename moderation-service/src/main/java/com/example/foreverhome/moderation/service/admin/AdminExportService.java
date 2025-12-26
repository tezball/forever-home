package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.AuditLog;
import com.example.foreverhome.moderation.repository.admin.AuditLogRepository;
import com.example.foreverhome.moderation.repository.admin.UserRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class AdminExportService {

    private final JdbcClient jdbcClient;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminExportService(JdbcClient jdbcClient,
                               UserRepository userRepository,
                               AuditLogRepository auditLogRepository) {
        this.jdbcClient = jdbcClient;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public String exportUsersToCsv(Map<String, String> filters) {
        StringWriter writer = new StringWriter();
        writer.append("ID,Email,Name,Role,Status,Created At,Last Login\n");

        String sql = """
            SELECT id, email, name, role, status, created_at, last_login_at
            FROM app_users
            ORDER BY created_at DESC
            """;

        jdbcClient.sql(sql)
                .query((rs, rowNum) -> {
                    writer.append(escapeCsv(rs.getString("id"))).append(",");
                    writer.append(escapeCsv(rs.getString("email"))).append(",");
                    writer.append(escapeCsv(rs.getString("name"))).append(",");
                    writer.append(escapeCsv(rs.getString("role"))).append(",");
                    writer.append(escapeCsv(rs.getString("status"))).append(",");
                    writer.append(formatTimestamp(rs.getTimestamp("created_at") != null ?
                            rs.getTimestamp("created_at").toInstant() : null)).append(",");
                    writer.append(formatTimestamp(rs.getTimestamp("last_login_at") != null ?
                            rs.getTimestamp("last_login_at").toInstant() : null)).append("\n");
                    return null;
                })
                .list();

        return writer.toString();
    }

    public String exportPetsToCsv(Map<String, String> filters) {
        StringWriter writer = new StringWriter();
        writer.append("ID,Name,Species,Breed,Age,Status,Rescue Org,Created At\n");

        String sql = """
            SELECT p.id, p.name, p.species, p.breed, p.age_text, p.status,
                   ro.organization_name as rescue_name, p.created_at
            FROM pets p
            LEFT JOIN rescue_organizations ro ON p.rescue_organization_id = ro.id
            ORDER BY p.created_at DESC
            """;

        jdbcClient.sql(sql)
                .query((rs, rowNum) -> {
                    writer.append(escapeCsv(rs.getString("id"))).append(",");
                    writer.append(escapeCsv(rs.getString("name"))).append(",");
                    writer.append(escapeCsv(rs.getString("species"))).append(",");
                    writer.append(escapeCsv(rs.getString("breed"))).append(",");
                    writer.append(escapeCsv(rs.getString("age_text"))).append(",");
                    writer.append(escapeCsv(rs.getString("status"))).append(",");
                    writer.append(escapeCsv(rs.getString("rescue_name"))).append(",");
                    writer.append(formatTimestamp(rs.getTimestamp("created_at") != null ?
                            rs.getTimestamp("created_at").toInstant() : null)).append("\n");
                    return null;
                })
                .list();

        return writer.toString();
    }

    public String exportAuditLogToCsv(Map<String, String> filters) {
        StringWriter writer = new StringWriter();
        writer.append("ID,Action,Entity Type,Entity ID,Actor ID,Details,Created At\n");

        List<AuditLog> logs = auditLogRepository.findAllPaged(10000, 0);
        for (AuditLog log : logs) {
            writer.append(escapeCsv(log.getId() != null ? log.getId().toString() : "")).append(",");
            writer.append(escapeCsv(log.getAction() != null ? log.getAction().name() : "")).append(",");
            writer.append(escapeCsv(log.getEntityType())).append(",");
            writer.append(escapeCsv(log.getEntityId() != null ? log.getEntityId().toString() : "")).append(",");
            writer.append(escapeCsv(log.getActorId() != null ? log.getActorId().toString() : "")).append(",");
            writer.append(escapeCsv(log.getDetails())).append(",");
            writer.append(formatTimestamp(log.getCreatedAt())).append("\n");
        }

        return writer.toString();
    }

    public String exportAdoptionsToCsv(Map<String, String> filters) {
        StringWriter writer = new StringWriter();
        writer.append("ID,Pet Name,Adopter Email,Rescue Org,Status,Applied At,Completed At\n");

        String sql = """
            SELECT aa.id, p.name as pet_name, u.email as adopter_email,
                   ro.organization_name as rescue_name, aa.status,
                   aa.created_at, aa.completed_at
            FROM adoption_applications aa
            JOIN pets p ON aa.pet_id = p.id
            JOIN app_users u ON aa.adopter_id = u.id
            LEFT JOIN rescue_organizations ro ON p.rescue_organization_id = ro.id
            ORDER BY aa.created_at DESC
            """;

        jdbcClient.sql(sql)
                .query((rs, rowNum) -> {
                    writer.append(escapeCsv(rs.getString("id"))).append(",");
                    writer.append(escapeCsv(rs.getString("pet_name"))).append(",");
                    writer.append(escapeCsv(rs.getString("adopter_email"))).append(",");
                    writer.append(escapeCsv(rs.getString("rescue_name"))).append(",");
                    writer.append(escapeCsv(rs.getString("status"))).append(",");
                    writer.append(formatTimestamp(rs.getTimestamp("created_at") != null ?
                            rs.getTimestamp("created_at").toInstant() : null)).append(",");
                    writer.append(formatTimestamp(rs.getTimestamp("completed_at") != null ?
                            rs.getTimestamp("completed_at").toInstant() : null)).append("\n");
                    return null;
                })
                .list();

        return writer.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatTimestamp(Instant instant) {
        if (instant == null) return "";
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
