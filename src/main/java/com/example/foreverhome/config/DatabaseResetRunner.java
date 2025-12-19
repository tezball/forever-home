package com.example.foreverhome.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resets the database before Flyway migrations when RESET_DATABASE=true.
 * This provides a Flyway callback that cleans the database before migrations run,
 * allowing the schema to be recreated from scratch and TestDataSeeder to re-seed demo data.
 */
@Configuration
public class DatabaseResetRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseResetRunner.class);

    @Value("${RESET_DATABASE:false}")
    private boolean resetDatabase;

    /**
     * Provides a Flyway callback that cleans the database before migration starts
     * when RESET_DATABASE=true.
     */
    @Bean
    public Callback databaseCleanCallback() {
        return new Callback() {
            @Override
            public boolean supports(Event event, Context context) {
                return event == Event.BEFORE_MIGRATE && resetDatabase;
            }

            @Override
            public boolean canHandleInTransaction(Event event, Context context) {
                return false;
            }

            @Override
            public void handle(Event event, Context context) {
                if (event == Event.BEFORE_MIGRATE && resetDatabase) {
                    logger.warn("RESET_DATABASE=true - Cleaning database before migration...");
                    Flyway flyway = Flyway.configure()
                            .configuration(context.getConfiguration())
                            .load();
                    flyway.clean();
                    logger.info("Database cleaned successfully");
                }
            }

            @Override
            public String getCallbackName() {
                return "DatabaseCleanCallback";
            }
        };
    }
}
