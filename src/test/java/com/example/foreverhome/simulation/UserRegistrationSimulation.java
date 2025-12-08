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
 * Gatling simulation for testing user registration with different user types.
 *
 * This simulation tests the registration endpoint with various user roles:
 * - FOSTER: Users who register pets for adoption
 * - ADOPTER: Users looking to adopt pets
 * - VET: Veterinarians who verify pet health
 * - RESCUE_ORG: Rescue organizations managing adoptions
 *
 * Run with: ./mvnw gatling:test
 *
 * Configuration via system properties:
 * - BASE_URL: Target URL (default: http://localhost:8080)
 * - USERS: Number of users per role (default: 10)
 * - RAMP_DURATION: Ramp-up duration in seconds (default: 10)
 */
public class UserRegistrationSimulation extends Simulation {

    // Configuration from system properties with defaults
    private static final String BASE_URL = System.getProperty("BASE_URL", "http://localhost:8080");
    private static final int USERS_PER_ROLE = Integer.parseInt(System.getProperty("USERS", "10"));
    private static final int RAMP_DURATION_SECONDS = Integer.parseInt(System.getProperty("RAMP_DURATION", "10"));

    // User roles to test (excluding ADMIN as it's typically created differently)
    private static final String[] USER_ROLES = {"FOSTER", "ADOPTER", "VET", "RESCUE_ORG"};

    // Atomic counter for unique user generation
    private static final AtomicInteger userCounter = new AtomicInteger(0);

    // HTTP protocol configuration
    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/UserRegistrationSimulation");

    // Feeder for generating unique user data for each role
    private Iterator<Map<String, Object>> userFeeder(String role) {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            int id = userCounter.incrementAndGet();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            return Map.of(
                    "email", "loadtest-" + role.toLowerCase() + "-" + id + "-" + uuid + "@test.com",
                    "password", "Password123!",
                    "name", "LoadTest " + capitalize(role) + " " + id,
                    "role", role
            );
        }).iterator();
    }

    // Helper to capitalize role names
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase().replace("_", " ");
    }

    // Registration request chain
    private ChainBuilder registerUser(String role) {
        return feed(userFeeder(role))
                .exec(http("Register " + role)
                        .post("/api/auth/register")
                        .body(StringBody("""
                                {
                                    "email": "#{email}",
                                    "password": "#{password}",
                                    "name": "#{name}",
                                    "role": "#{role}"
                                }
                                """))
                        .check(status().in(201, 409)) // 201 Created or 409 Conflict (already exists)
                        .check(status().saveAs("responseStatus"))
                )
                .exec(session -> {
                    int status = session.getInt("responseStatus");
                    String email = session.getString("email");
                    if (status == 201) {
                        System.out.println("Successfully registered: " + email);
                    } else {
                        System.out.println("User already exists or error: " + email);
                    }
                    return session;
                });
    }

    // Scenario for registering Foster users
    private ScenarioBuilder fosterScenario = scenario("Register Foster Users")
            .exec(registerUser("FOSTER"));

    // Scenario for registering Adopter users
    private ScenarioBuilder adopterScenario = scenario("Register Adopter Users")
            .exec(registerUser("ADOPTER"));

    // Scenario for registering Vet users
    private ScenarioBuilder vetScenario = scenario("Register Vet Users")
            .exec(registerUser("VET"));

    // Scenario for registering Rescue Org users
    private ScenarioBuilder rescueOrgScenario = scenario("Register Rescue Org Users")
            .exec(registerUser("RESCUE_ORG"));

    // Mixed scenario that randomly selects user types
    private ScenarioBuilder mixedScenario = scenario("Register Mixed User Types")
            .randomSwitch().on(
                    percent(30.0).then(exec(registerUser("ADOPTER"))),    // 30% adopters (most common)
                    percent(25.0).then(exec(registerUser("FOSTER"))),    // 25% fosters
                    percent(25.0).then(exec(registerUser("VET"))),       // 25% vets
                    percent(20.0).then(exec(registerUser("RESCUE_ORG"))) // 20% rescue orgs
            );

    // Simulation setup with multiple injection profiles
    {
        setUp(
                // Individual role scenarios - run sequentially with ramp-up
                fosterScenario.injectOpen(
                        rampUsers(USERS_PER_ROLE).during(Duration.ofSeconds(RAMP_DURATION_SECONDS))
                ),
                adopterScenario.injectOpen(
                        rampUsers(USERS_PER_ROLE).during(Duration.ofSeconds(RAMP_DURATION_SECONDS))
                ),
                vetScenario.injectOpen(
                        rampUsers(USERS_PER_ROLE).during(Duration.ofSeconds(RAMP_DURATION_SECONDS))
                ),
                rescueOrgScenario.injectOpen(
                        rampUsers(USERS_PER_ROLE).during(Duration.ofSeconds(RAMP_DURATION_SECONDS))
                )
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().max().lt(5000),      // Max response time < 5s
                        global().successfulRequests().percent().gt(95.0) // >95% success rate
                );
    }
}
