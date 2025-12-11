package com.example.foreverhome.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom application metrics for Prometheus monitoring.
 * These metrics track business-relevant events like adoptions, applications, and user registrations.
 * Note: This config is excluded in static profile as it requires database access.
 */
@Configuration
@Profile("!static")
public class MetricsConfig {

    private final AtomicLong activeApplications = new AtomicLong(0);

    @Bean
    public Counter adoptionsCompletedCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.adoptions.completed")
                .description("Total number of completed adoptions")
                .register(registry);
    }

    @Bean
    public Counter applicationsSubmittedCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.applications.submitted")
                .description("Total number of adoption applications submitted")
                .register(registry);
    }

    @Bean
    public Counter applicationsApprovedCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.applications.approved")
                .description("Total number of adoption applications approved")
                .register(registry);
    }

    @Bean
    public Counter applicationsRejectedCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.applications.rejected")
                .description("Total number of adoption applications rejected")
                .register(registry);
    }

    @Bean
    public Counter userRegistrationsCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.users.registrations.total")
                .description("Total number of user registrations")
                .register(registry);
    }

    @Bean
    public Counter petRegistrationsCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.pets.registrations")
                .description("Total number of pets registered")
                .register(registry);
    }

    @Bean
    public Counter vetSignOffsCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.vet.signoffs")
                .description("Total number of vet sign-offs")
                .register(registry);
    }

    @Bean
    public Counter emailsSentCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.emails.sent")
                .description("Total number of emails sent")
                .register(registry);
    }

    @Bean
    public Counter emailsFailedCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.emails.failed")
                .description("Total number of failed email sends")
                .register(registry);
    }

    @Bean
    public Counter rateLimitHitsCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.ratelimit.hits")
                .description("Total number of rate limit violations")
                .register(registry);
    }

    @Bean
    public Timer applicationProcessingTimer(MeterRegistry registry) {
        return Timer.builder("foreverhome.application.processing.time")
                .description("Time taken to process adoption applications")
                .register(registry);
    }

    @Bean
    public Gauge activeUsersGauge(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        return Gauge.builder("foreverhome.users.active", () -> {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM app_users WHERE status = 'ACTIVE'",
                        Integer.class
                );
                return count != null ? count : 0;
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Number of active users")
        .register(registry);
    }

    @Bean
    public Gauge availablePetsGauge(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        return Gauge.builder("foreverhome.pets.available", () -> {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM pets WHERE status = 'AVAILABLE'",
                        Integer.class
                );
                return count != null ? count : 0;
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Number of pets currently available for adoption")
        .register(registry);
    }

    @Bean
    public Gauge pendingApplicationsGauge(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        return Gauge.builder("foreverhome.applications.pending", () -> {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM adoption_applications WHERE status IN ('SUBMITTED', 'UNDER_REVIEW')",
                        Integer.class
                );
                return count != null ? count : 0;
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Number of pending adoption applications")
        .register(registry);
    }

    @Bean
    public Gauge pendingRescueApprovals(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        return Gauge.builder("foreverhome.rescueorgs.pending", () -> {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM rescue_organizations WHERE verified = false",
                        Integer.class
                );
                return count != null ? count : 0;
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Number of rescue organizations pending approval")
        .register(registry);
    }

    // Content moderation metrics

    @Bean
    public Gauge pendingFlagsGauge(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        return Gauge.builder("foreverhome.flags.pending", () -> {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM content_flags WHERE status = 'PENDING'",
                        Integer.class
                );
                return count != null ? count : 0;
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Number of pending content flags")
        .register(registry);
    }

    @Bean
    public Counter flagsCreatedCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.flags.created")
                .description("Total number of content flags created")
                .register(registry);
    }

    @Bean
    public Counter flagsResolvedCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.flags.resolved")
                .description("Total number of content flags resolved")
                .register(registry);
    }

    // User management metrics

    @Bean
    public Gauge suspendedUsersGauge(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        return Gauge.builder("foreverhome.users.suspended", () -> {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM app_users WHERE status = 'SUSPENDED'",
                        Integer.class
                );
                return count != null ? count : 0;
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Number of suspended users")
        .register(registry);
    }

    @Bean
    public Counter adminActionsCounter(MeterRegistry registry) {
        return Counter.builder("foreverhome.admin.actions")
                .description("Total number of admin actions performed")
                .register(registry);
    }
}
