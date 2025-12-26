package com.example.foreverhome.moderation.service.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStorageService {

    private final S3Client s3Client;
    private final JdbcClient jdbcClient;
    private final String bucketName;

    public AdminStorageService(S3Client s3Client,
                                JdbcClient jdbcClient,
                                @Value("${aws.s3.bucket-name:forever-home-images}") String bucketName) {
        this.s3Client = s3Client;
        this.jdbcClient = jdbcClient;
        this.bucketName = bucketName;
    }

    public StorageDashboard getDashboard() {
        S3Stats s3Stats = getS3Stats();
        EmailStats emailStats = getEmailStats();
        StorageCostEstimate costEstimate = estimateCosts(s3Stats, emailStats);

        return new StorageDashboard(s3Stats, emailStats, costEstimate);
    }

    public S3Stats getS3Stats() {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            long totalObjects = 0;
            long totalSizeBytes = 0;
            Map<String, Long> objectsByType = new HashMap<>();

            ListObjectsV2Response response;
            do {
                response = s3Client.listObjectsV2(request);

                for (S3Object object : response.contents()) {
                    totalObjects++;
                    totalSizeBytes += object.size();

                    String key = object.key();
                    String type = categorizeObject(key);
                    objectsByType.merge(type, 1L, Long::sum);
                }

                request = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .continuationToken(response.nextContinuationToken())
                        .build();

            } while (response.isTruncated());

            return new S3Stats(
                    bucketName,
                    totalObjects,
                    totalSizeBytes,
                    formatBytes(totalSizeBytes),
                    objectsByType
            );
        } catch (Exception e) {
            return new S3Stats(bucketName, 0, 0, "0 B", new HashMap<>());
        }
    }

    private String categorizeObject(String key) {
        if (key.contains("pet-images/")) return "Pet Images";
        if (key.contains("logos/")) return "Organization Logos";
        if (key.contains("avatars/")) return "User Avatars";
        return "Other";
    }

    public EmailStats getEmailStats() {
        // Query email logs from database if available
        long totalSent = countEmailsByType("all");
        long sentToday = countEmailsSentToday();

        Map<String, Long> emailsByType = new HashMap<>();
        emailsByType.put("notification", countEmailsByType("notification"));
        emailsByType.put("verification", countEmailsByType("verification"));
        emailsByType.put("password_reset", countEmailsByType("password_reset"));
        emailsByType.put("application", countEmailsByType("application"));

        return new EmailStats(totalSent, sentToday, emailsByType);
    }

    private long countEmailsByType(String type) {
        // Placeholder - would query actual email logs table
        try {
            if ("all".equals(type)) {
                return jdbcClient.sql("SELECT COUNT(*) FROM email_logs")
                        .query(Long.class)
                        .optional()
                        .orElse(0L);
            }
            return jdbcClient.sql("SELECT COUNT(*) FROM email_logs WHERE email_type = :type")
                    .param("type", type)
                    .query(Long.class)
                    .optional()
                    .orElse(0L);
        } catch (Exception e) {
            // Table might not exist yet
            return 0L;
        }
    }

    private long countEmailsSentToday() {
        try {
            return jdbcClient.sql("""
                SELECT COUNT(*) FROM email_logs
                WHERE sent_at >= CURRENT_DATE
                """)
                    .query(Long.class)
                    .optional()
                    .orElse(0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    public StorageCostEstimate estimateCosts(S3Stats s3Stats, EmailStats emailStats) {
        // AWS pricing estimates (approximate)
        double s3StorageCostPerGb = 0.023; // S3 Standard per GB-month
        double sesCostPerEmail = 0.0001; // $0.10 per 1000 emails

        double s3Gigabytes = (double) s3Stats.totalSizeBytes() / (1024 * 1024 * 1024);
        double s3MonthlyCost = s3Gigabytes * s3StorageCostPerGb;

        double emailMonthlyCost = emailStats.totalSent() * sesCostPerEmail;

        return new StorageCostEstimate(
                s3MonthlyCost,
                emailMonthlyCost,
                s3MonthlyCost + emailMonthlyCost
        );
    }

    public List<RecentUpload> getRecentUploads(int limit) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(limit)
                    .build();

            List<RecentUpload> uploads = new ArrayList<>();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            for (S3Object object : response.contents()) {
                uploads.add(new RecentUpload(
                        object.key(),
                        object.size(),
                        formatBytes(object.size()),
                        object.lastModified()
                ));
            }

            return uploads;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public record StorageDashboard(
            S3Stats s3Stats,
            EmailStats emailStats,
            StorageCostEstimate costEstimate
    ) {}

    public record S3Stats(
            String bucketName,
            long totalObjects,
            long totalSizeBytes,
            String totalSizePretty,
            Map<String, Long> objectsByType
    ) {}

    public record EmailStats(
            long totalSent,
            long sentToday,
            Map<String, Long> emailsByType
    ) {}

    public record StorageCostEstimate(
            double s3MonthlyCost,
            double emailMonthlyCost,
            double totalMonthlyCost
    ) {}

    public record RecentUpload(
            String key,
            long sizeBytes,
            String sizePretty,
            Instant lastModified
    ) {}
}
