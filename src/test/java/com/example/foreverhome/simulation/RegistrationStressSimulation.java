package com.example.foreverhome.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Stress test simulation for user registration.
 *
 * This simulation tests the registration endpoint under various load conditions:
 * - Spike test: Sudden burst of registrations
 * - Ramp-up test: Gradually increasing load
 * - Sustained load: Constant rate of registrations
 *
 * Run with: ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.RegistrationStressSimulation
 *
 * Configuration via system properties:
 * - BASE_URL: Target URL (default: http://localhost:8080)
 * - PEAK_USERS: Peak concurrent users (default: 50)
 * - DURATION: Test duration in seconds (default: 60)
 */
public class RegistrationStressSimulation extends Simulation {

    // Configuration
    private static final String BASE_URL = System.getProperty("BASE_URL", "http://localhost:8080");
    private static final int PEAK_USERS = Integer.parseInt(System.getProperty("PEAK_USERS", "50"));
    private static final int DURATION_SECONDS = Integer.parseInt(System.getProperty("DURATION", "60"));

    private static final String[] USER_ROLES = {"FOSTER", "ADOPTER", "VET", "RESCUE_ORG"};
    private static final AtomicInteger userCounter = new AtomicInteger(0);

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/RegistrationStressSimulation")
            .shareConnections(); // Share connections for higher throughput

    // Feeder generating random user types
    private Iterator<Map<String, Object>> randomUserFeeder() {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            int id = userCounter.incrementAndGet();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            String role = USER_ROLES[(int) (Math.random() * USER_ROLES.length)];
            return Map.of(
                    "email", "stress-" + id + "-" + uuid + "@loadtest.com",
                    "password", "StressTest123!",
                    "name", "Stress Test User " + id,
                    "role", role
            );
        }).iterator();
    }

    // Registration chain with validation
    private ChainBuilder registerRandomUser = feed(randomUserFeeder())
            .exec(http("Register User - #{role}")
                    .post("/api/auth/register")
                    .body(StringBody("""
                            {
                                "email": "#{email}",
                                "password": "#{password}",
                                "name": "#{name}",
                                "role": "#{role}"
                            }
                            """))
                    .check(status().in(201, 409, 400)) // Accept created, conflict, or validation errors
            );

    // Scenario with think time to simulate real user behavior
    private ScenarioBuilder realisticRegistration = scenario("Realistic Registration")
            .exec(registerRandomUser)
            .pause(Duration.ofMillis(100), Duration.ofMillis(500)); // Random pause between 100-500ms

    // Scenario without think time for maximum stress
    private ScenarioBuilder maxStressRegistration = scenario("Max Stress Registration")
            .exec(registerRandomUser);

    // Spike test scenario - burst of users
    private ScenarioBuilder spikeTest = scenario("Spike Test Registration")
            .exec(registerRandomUser);

    {
        setUp(
                // Ramp up to peak, sustain, then ramp down (realistic load test)
                realisticRegistration.injectOpen(
                        rampUsersPerSec(1).to(PEAK_USERS / 2.0).during(Duration.ofSeconds(DURATION_SECONDS / 4)),
                        constantUsersPerSec(PEAK_USERS / 2.0).during(Duration.ofSeconds(DURATION_SECONDS / 2)),
                        rampUsersPerSec(PEAK_USERS / 2.0).to(1).during(Duration.ofSeconds(DURATION_SECONDS / 4))
                ),

                // Spike test - sudden burst after 10 seconds
                spikeTest.injectOpen(
                        nothingFor(Duration.ofSeconds(10)),
                        atOnceUsers(PEAK_USERS / 2)
                )
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().percentile3().lt(3000), // 95th percentile < 3s
                        global().failedRequests().percent().lt(10.0)   // <10% failures allowed for stress test
                );
    }
}
