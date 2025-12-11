package com.example.foreverhome.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

/**
 * Ensures Flyway migrations run at application startup.
 * This explicitly configures and runs Flyway to ensure database schema is created
 * before any other beans that depend on the database tables.
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

    /**
     * Explicitly configure and run Flyway migrations.
     */
    @Bean(initMethod = "migrate")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Flyway flyway(DataSource dataSource) {
        if (!flywayEnabled) {
            logger.info("Flyway is disabled, skipping migrations");
            return Flyway.configure().dataSource(dataSource).load();
        }

        logger.info("Configuring Flyway manually with datasource");
        logger.info("Flyway locations: {}", locations);
        logger.info("Baseline on migrate: {}, Baseline version: {}", baselineOnMigrate, baselineVersion);

        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(locations.split(","))
            .baselineOnMigrate(baselineOnMigrate)
            .baselineVersion(baselineVersion)
            .load();

        logger.info("Flyway configured. Running migrations via initMethod...");
        return flyway;
    }
}
