package com.example.foreverhome.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

/**
 * Ensures Flyway migrations run at application startup.
 * This explicitly configures and runs Flyway to ensure database schema is created
 * before any other beans that depend on the database tables.
 *
 * Supports optional database reset via clean-on-start property for development/testing.
 */
@Configuration
public class FlywayMigrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(FlywayMigrationRunner.class);

    @Value("${spring.flyway.enabled:true}")
    private boolean flywayEnabled;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:0}")
    private String baselineVersion;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String locations;

    @Value("${spring.flyway.clean-on-start:false}")
    private boolean cleanOnStart;

    /**
     * Explicitly configure and run Flyway migrations.
     * If clean-on-start is enabled, drops all tables before migrating.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Flyway flyway(DataSource dataSource) {
        if (!flywayEnabled) {
            logger.info("Flyway is disabled, skipping migrations");
            return Flyway.configure().dataSource(dataSource).load();
        }

        logger.info("Configuring Flyway manually with datasource");
        logger.info("Flyway locations: {}", locations);
        logger.info("Baseline on migrate: {}, Baseline version: {}", baselineOnMigrate, baselineVersion);
        logger.info("Clean on start: {}", cleanOnStart);

        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(locations.split(","))
            .baselineOnMigrate(baselineOnMigrate)
            .baselineVersion(baselineVersion)
            .cleanDisabled(false) // Enable clean command
            .load();

        if (cleanOnStart) {
            logger.warn("!!! FLYWAY CLEAN ENABLED - DROPPING ALL TABLES !!!");
            flyway.clean();
            logger.info("Database cleaned successfully");
        }

        logger.info("Running Flyway migrations...");
        flyway.migrate();
        logger.info("Flyway migrations completed");

        return flyway;
    }
}
