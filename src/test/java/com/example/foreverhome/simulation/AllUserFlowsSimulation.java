package com.example.foreverhome.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Comprehensive Gatling simulation that tests ALL user flows in the Forever Home application.
 *
 * This simulation covers:
 * - Authentication (register, login, logout, refresh token, password reset)
 * - Foster flows (create pet, update pet, submit for review, withdraw)
 * - Adopter flows (browse pets, favorites, applications, adoptions)
 * - Rescue Org flows (accept/decline pets, manage vets, process applications)
 * - Vet flows (lookup pets, sign-off)
 * - Admin flows (analytics, moderation, user management, approvals)
 * - Public flows (browse pets, view rescue profiles, view vet profiles)
 * - Notifications (get, mark read, preferences)
 *
 * Run with: ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.AllUserFlowsSimulation
 *
 * Configuration via system properties:
 * - BASE_URL: Target URL (default: http://localhost:8080)
 * - USERS: Users per scenario (default: 5)
 * - RAMP_DURATION: Ramp-up duration in seconds (default: 30)
 * - TEST_DURATION: Total test duration in seconds (default: 120)
 * - ADMIN_EMAIL: Admin email (default: admin@foreverhome.com)
 * - ADMIN_PASSWORD: Admin password (default: AdminPass123!)
 */
public class AllUserFlowsSimulation extends Simulation {

    // Configuration
    private static final String BASE_URL = System.getProperty("BASE_URL", "http://localhost:8080");
    private static final int USERS_PER_SCENARIO = Integer.parseInt(System.getProperty("USERS", "5"));
    private static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION", "30"));
    private static final int TEST_DURATION = Integer.parseInt(System.getProperty("TEST_DURATION", "120"));
    private static final String ADMIN_EMAIL = System.getProperty("ADMIN_EMAIL", "admin@test.com");
    private static final String ADMIN_PASSWORD = System.getProperty("ADMIN_PASSWORD", "password123");

    // Counters for unique data generation
    private static final AtomicInteger userCounter = new AtomicInteger(0);
    private static final AtomicInteger petCounter = new AtomicInteger(0);
    private static final AtomicInteger microchipCounter = new AtomicInteger(0);

    // Test data arrays
    private static final String[] SPECIES = {"DOG", "CAT", "BIRD", "RABBIT"};
    private static final String[] SIZES = {"SMALL", "MEDIUM", "LARGE"};
    private static final String[] SEXES = {"MALE", "FEMALE"};
    private static final String[] AGE_UNITS = {"MONTHS", "YEARS"};
    private static final String[] PET_NAMES = {"Max", "Bella", "Charlie", "Luna", "Cooper", "Daisy", "Buddy", "Sadie"};
    private static final String[] BREEDS = {"Labrador", "Poodle", "Persian", "Siamese", "Beagle", "Golden Retriever"};

    // HTTP Protocol
    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/AllUserFlowsSimulation")
            .shareConnections();

    // ==================== DATA FEEDERS ====================

    private Iterator<Map<String, Object>> userFeeder(String role) {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            int id = userCounter.incrementAndGet();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            return Map.of(
                    "email", "allflows-" + role.toLowerCase() + "-" + id + "-" + uuid + "@test.com",
                    "password", "TestPass123!",
                    "name", "AllFlows " + role + " " + id,
                    "role", role
            );
        }).iterator();
    }

    private Iterator<Map<String, Object>> petFeeder() {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            int id = petCounter.incrementAndGet();
            String species = SPECIES[(int) (Math.random() * SPECIES.length)];
            return Map.of(
                    "petName", PET_NAMES[(int) (Math.random() * PET_NAMES.length)] + "-" + id,
                    "species", species,
                    "breed", BREEDS[(int) (Math.random() * BREEDS.length)],
                    "age", (int) (Math.random() * 10) + 1,
                    "ageUnit", AGE_UNITS[(int) (Math.random() * AGE_UNITS.length)],
                    "sex", SEXES[(int) (Math.random() * SEXES.length)],
                    "size", SIZES[(int) (Math.random() * SIZES.length)],
                    "microchipId", "CHIP-FLOW-" + microchipCounter.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8)
            );
        }).iterator();
    }

    // ==================== AUTHENTICATION CHAINS ====================

    private ChainBuilder register(String role) {
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
                        .check(status().in(201, 409))
                        .check(jsonPath("$.accessToken").optional().saveAs("accessToken"))
                        .check(jsonPath("$.refreshToken").optional().saveAs("refreshToken"))
                        .check(jsonPath("$.user.id").optional().saveAs("userId"))
                );
    }

    private ChainBuilder login() {
        return exec(http("Login")
                .post("/api/auth/login")
                .body(StringBody("""
                        {
                            "email": "#{email}",
                            "password": "#{password}"
                        }
                        """))
                .check(status().in(200, 401))
                .check(jsonPath("$.accessToken").optional().saveAs("accessToken"))
                .check(jsonPath("$.refreshToken").optional().saveAs("refreshToken"))
                .check(jsonPath("$.user.id").optional().saveAs("userId"))
        );
    }

    private ChainBuilder refreshToken() {
        return exec(http("Refresh Token")
                .post("/api/auth/refresh")
                .body(StringBody("""
                        {
                            "refreshToken": "#{refreshToken}"
                        }
                        """))
                .check(status().in(200, 401))
                .check(jsonPath("$.accessToken").optional().saveAs("accessToken"))
        );
    }

    private ChainBuilder logout() {
        return exec(http("Logout")
                .post("/api/auth/logout")
                .body(StringBody("""
                        {
                            "refreshToken": "#{refreshToken}"
                        }
                        """))
                .check(status().in(204, 400, 401))
        );
    }

    private ChainBuilder forgotPassword() {
        return exec(http("Forgot Password")
                .post("/api/auth/forgot-password")
                .body(StringBody("""
                        {
                            "email": "#{email}"
                        }
                        """))
                .check(status().in(200, 400, 404))
        );
    }

    private ChainBuilder setAuthHeader() {
        return exec(session -> session.set("authHeader", "Bearer " + session.getString("accessToken")));
    }

    // ==================== PUBLIC ENDPOINTS ====================

    private ChainBuilder browseAvailablePets() {
        return exec(http("Browse Available Pets")
                .get("/api/pets")
                .check(status().is(200))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("availablePetIds"))
        );
    }

    private ChainBuilder browseAvailablePetsWithFilters() {
        return exec(http("Browse Pets with Filters")
                .get("/api/pets")
                .queryParam("species", "DOG")
                .queryParam("size", "MEDIUM")
                .check(status().is(200))
        );
    }

    private ChainBuilder getFeaturedPets() {
        return exec(http("Get Featured Pets")
                .get("/api/pets/featured")
                .check(status().in(200, 404))
        );
    }

    private ChainBuilder getPlatformStats() {
        return exec(http("Get Platform Stats")
                .get("/api/stats")
                .check(status().in(200, 404))
        );
    }

    private ChainBuilder getPetDetails() {
        return doIf(session -> session.contains("availablePetIds") &&
                !((List<?>) session.get("availablePetIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> petIds = session.getList("availablePetIds");
                            String randomPetId = petIds.get((int) (Math.random() * petIds.size()));
                            return session.set("selectedPetId", randomPetId);
                        })
                        .exec(http("Get Pet Details")
                                .get("/api/pets/#{selectedPetId}")
                                .check(status().in(200, 404))
                                .check(jsonPath("$.microchipId").optional().saveAs("petMicrochip"))
                        )
                );
    }

    private ChainBuilder listRescueOrgsPublic() {
        return exec(http("List Rescue Orgs (Public)")
                .get("/api/rescue-orgs")
                .check(status().in(200, 404))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("rescueOrgIds"))
        );
    }

    private ChainBuilder getRescueOrgProfile() {
        return doIf(session -> session.contains("rescueOrgIds") &&
                !((List<?>) session.get("rescueOrgIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> orgIds = session.getList("rescueOrgIds");
                            return session.set("selectedRescueOrgId", orgIds.get(0));
                        })
                        .exec(http("Get Rescue Org Profile")
                                .get("/api/rescue-orgs/#{selectedRescueOrgId}")
                                .check(status().in(200, 404))
                        )
                        .exec(http("Get Rescue Org Pets")
                                .get("/api/rescue-orgs/#{selectedRescueOrgId}/pets")
                                .check(status().in(200, 404))
                        )
                );
    }

    private ChainBuilder listVetsPublic() {
        return exec(http("List Vets (Public)")
                .get("/api/vets")
                .check(status().in(200, 404))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("vetIds"))
        );
    }

    private ChainBuilder getVetProfile() {
        return doIf(session -> session.contains("vetIds") &&
                !((List<?>) session.get("vetIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> vetIds = session.getList("vetIds");
                            return session.set("selectedVetId", vetIds.get(0));
                        })
                        .exec(http("Get Vet Profile")
                                .get("/api/vets/#{selectedVetId}")
                                .check(status().in(200, 404))
                        )
                );
    }

    // ==================== NOTIFICATION CHAINS ====================

    private ChainBuilder getNotifications() {
        return exec(http("Get Notifications")
                .get("/api/notifications")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("notificationIds"))
        );
    }

    private ChainBuilder getUnreadCount() {
        return exec(http("Get Unread Notification Count")
                .get("/api/notifications/unread/count")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder markNotificationRead() {
        return doIf(session -> session.contains("notificationIds") &&
                !((List<?>) session.get("notificationIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> ids = session.getList("notificationIds");
                            return session.set("notificationId", ids.get(0));
                        })
                        .exec(http("Mark Notification Read")
                                .put("/api/notifications/#{notificationId}/read")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(204, 401, 403, 404))
                        )
                );
    }

    // ==================== PROFILE/SETTINGS CHAINS ====================

    private ChainBuilder getProfile() {
        return exec(http("Get Profile")
                .get("/api/profile")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder getSettings() {
        return exec(http("Get Settings")
                .get("/api/settings")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403, 404))
        );
    }

    private ChainBuilder updateNotificationPreferences() {
        return exec(http("Update Notification Preferences")
                .put("/api/settings/notifications")
                .header("Authorization", "#{authHeader}")
                .body(StringBody("""
                        {
                            "emailNotifications": true,
                            "inAppNotifications": true
                        }
                        """))
                .check(status().in(200, 204, 400, 401, 403, 404))
        );
    }

    // ==================== FOSTER JOURNEY CHAINS ====================

    private ChainBuilder completeFosterProfile() {
        return exec(http("Complete Foster Profile")
                .put("/api/profile/foster")
                .header("Authorization", "#{authHeader}")
                .body(StringBody("""
                        {
                            "firstName": "Test",
                            "lastName": "Foster",
                            "phone": "555-1234",
                            "address": "123 Foster Lane"
                        }
                        """))
                .check(status().in(200, 400, 401, 403))
        );
    }

    private ChainBuilder createPet() {
        return feed(petFeeder())
                .exec(http("Create Pet")
                        .post("/api/pets")
                        .header("Authorization", "#{authHeader}")
                        .body(StringBody("""
                                {
                                    "name": "#{petName}",
                                    "species": "#{species}",
                                    "breed": "#{breed}",
                                    "age": #{age},
                                    "ageUnit": "#{ageUnit}",
                                    "sex": "#{sex}",
                                    "size": "#{size}",
                                    "microchipId": "#{microchipId}",
                                    "description": "A lovely pet looking for a forever home",
                                    "healthNotes": "Healthy and vaccinated"
                                }
                                """))
                        .check(status().in(201, 400, 401, 403))
                        .check(jsonPath("$.id").optional().saveAs("createdPetId"))
                        .check(jsonPath("$.microchipId").optional().saveAs("createdPetMicrochip"))
                );
    }

    private ChainBuilder updatePet() {
        return doIf(session -> session.contains("createdPetId"))
                .then(
                        exec(http("Update Pet")
                                .put("/api/pets/#{createdPetId}")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "description": "Updated description - even more lovely!"
                                        }
                                        """))
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder getMyPets() {
        return exec(http("Get My Pets")
                .get("/api/pets/my")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("myPetIds"))
        );
    }

    private ChainBuilder submitPetForReview() {
        return doIf(session -> session.contains("createdPetId") && session.contains("rescueOrgId"))
                .then(
                        exec(http("Submit Pet for Review")
                                .post("/api/pets/#{createdPetId}/submit")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "rescueOrgId": "#{rescueOrgId}"
                                        }
                                        """))
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder withdrawPet() {
        return doIf(session -> session.contains("createdPetId"))
                .then(
                        exec(http("Withdraw Pet")
                                .post("/api/pets/#{createdPetId}/withdraw")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    // ==================== ADOPTER JOURNEY CHAINS ====================

    private ChainBuilder completeAdopterProfile() {
        return exec(http("Complete Adopter Profile")
                .put("/api/profile/adopter")
                .header("Authorization", "#{authHeader}")
                .body(StringBody("""
                        {
                            "firstName": "Test",
                            "lastName": "Adopter",
                            "phone": "555-5678",
                            "address": "456 Adopter Ave",
                            "housingType": "HOUSE",
                            "hasYard": true,
                            "hasPets": false,
                            "hasChildren": false
                        }
                        """))
                .check(status().in(200, 400, 401, 403))
        );
    }

    private ChainBuilder addFavorite() {
        return doIf(session -> session.contains("selectedPetId"))
                .then(
                        exec(http("Add Favorite")
                                .post("/api/favorites/#{selectedPetId}")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(201, 400, 401, 403, 404, 409))
                        )
                );
    }

    private ChainBuilder getFavorites() {
        return exec(http("Get Favorites")
                .get("/api/favorites")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder removeFavorite() {
        return doIf(session -> session.contains("selectedPetId"))
                .then(
                        exec(http("Remove Favorite")
                                .delete("/api/favorites/#{selectedPetId}")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(204, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder submitApplication() {
        return doIf(session -> session.contains("selectedPetId"))
                .then(
                        exec(http("Submit Adoption Application")
                                .post("/api/applications")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "petId": "#{selectedPetId}",
                                            "message": "I would love to adopt this pet!",
                                            "livingSituation": "House with yard",
                                            "petExperience": "5 years with dogs",
                                            "whyAdopt": "Looking for a companion"
                                        }
                                        """))
                                .check(status().in(201, 400, 401, 403, 404, 409))
                                .check(jsonPath("$.id").optional().saveAs("applicationId"))
                        )
                );
    }

    private ChainBuilder getMyApplications() {
        return exec(http("Get My Applications")
                .get("/api/applications")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("myApplicationIds"))
        );
    }

    private ChainBuilder withdrawApplication() {
        return doIf(session -> session.contains("applicationId"))
                .then(
                        exec(http("Withdraw Application")
                                .delete("/api/applications/#{applicationId}")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(204, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder getMyAdoptions() {
        return exec(http("Get My Adoptions")
                .get("/api/adoptions")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
        );
    }

    // ==================== RESCUE ORG JOURNEY CHAINS ====================

    private ChainBuilder completeRescueOrgProfile() {
        return exec(http("Complete Rescue Org Profile")
                .put("/api/profile/rescue-org")
                .header("Authorization", "#{authHeader}")
                .body(StringBody("""
                        {
                            "organizationName": "Test Rescue Org",
                            "description": "Helping pets find forever homes",
                            "phone": "555-RESCUE",
                            "website": "https://testrescue.org",
                            "address": "789 Rescue Road"
                        }
                        """))
                .check(status().in(200, 400, 401, 403))
        );
    }

    private ChainBuilder listRescueOrgs() {
        return exec(http("List Rescue Orgs")
                .get("/api/rescue-org/list")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("rescueOrgList"))
        );
    }

    private ChainBuilder getPendingPets() {
        return exec(http("Get Pending Pets for Rescue")
                .get("/api/pets/rescue/pending")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("pendingPetIds"))
        );
    }

    private ChainBuilder acceptPet() {
        return doIf(session -> session.contains("pendingPetIds") &&
                !((List<?>) session.get("pendingPetIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> petIds = session.getList("pendingPetIds");
                            return session.set("petToAccept", petIds.get(0));
                        })
                        .exec(http("Accept Pet")
                                .post("/api/pets/#{petToAccept}/accept")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder declinePet() {
        return doIf(session -> session.contains("pendingPetIds") &&
                ((List<?>) session.get("pendingPetIds")).size() > 1)
                .then(
                        exec(session -> {
                            List<String> petIds = session.getList("pendingPetIds");
                            return session.set("petToDecline", petIds.get(1));
                        })
                        .exec(http("Decline Pet")
                                .post("/api/pets/#{petToDecline}/decline")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "reason": "Not suitable for our rescue at this time"
                                        }
                                        """))
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder getPendingVets() {
        return exec(http("Get Pending Vets")
                .get("/api/rescue-org/vets/pending")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("pendingVetIds"))
        );
    }

    private ChainBuilder getApprovedVets() {
        return exec(http("Get Approved Vets")
                .get("/api/rescue-org/vets/approved")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder approveVet() {
        return doIf(session -> session.contains("pendingVetIds") &&
                !((List<?>) session.get("pendingVetIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> vetIds = session.getList("pendingVetIds");
                            return session.set("vetToApprove", vetIds.get(0));
                        })
                        .exec(http("Approve Vet")
                                .post("/api/rescue-org/vets/#{vetToApprove}/approve")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(201, 400, 401, 403, 404, 409))
                        )
                );
    }

    private ChainBuilder getApplicationsForRescue() {
        return exec(http("Get Applications for Rescue")
                .get("/api/applications/rescue")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("rescueApplicationIds"))
        );
    }

    private ChainBuilder approveApplication() {
        return doIf(session -> session.contains("rescueApplicationIds") &&
                !((List<?>) session.get("rescueApplicationIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> appIds = session.getList("rescueApplicationIds");
                            return session.set("appToApprove", appIds.get(0));
                        })
                        .exec(http("Approve Application")
                                .put("/api/applications/#{appToApprove}/approve")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    // ==================== VET JOURNEY CHAINS ====================

    private ChainBuilder completeVetProfile() {
        return exec(http("Complete Vet Profile")
                .put("/api/profile/vet")
                .header("Authorization", "#{authHeader}")
                .body(StringBody("""
                        {
                            "clinicName": "Test Vet Clinic",
                            "licenseNumber": "VET-123456",
                            "phone": "555-VET1",
                            "address": "321 Vet Street"
                        }
                        """))
                .check(status().in(200, 400, 401, 403))
        );
    }

    private ChainBuilder lookupPetByMicrochip() {
        return doIf(session -> session.contains("petMicrochip"))
                .then(
                        exec(http("Lookup Pet by Microchip")
                                .get("/api/vet/pets/lookup")
                                .header("Authorization", "#{authHeader}")
                                .queryParam("microchip", "#{petMicrochip}")
                                .check(status().in(200, 401, 403, 404))
                                .check(jsonPath("$.id").optional().saveAs("lookedUpPetId"))
                        )
                );
    }

    private ChainBuilder vetSignOff() {
        return doIf(session -> session.contains("lookedUpPetId"))
                .then(
                        exec(http("Vet Sign Off")
                                .post("/api/vet/pets/#{lookedUpPetId}/sign-off")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "isNeutered": true,
                                            "isVaccinated": true,
                                            "isHealthy": true,
                                            "healthNotes": "Pet is in excellent health"
                                        }
                                        """))
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder getVetSignOffHistory() {
        return exec(http("Get Vet Sign-Off History")
                .get("/api/vet/sign-offs")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
        );
    }

    // ==================== ADMIN JOURNEY CHAINS ====================

    private ChainBuilder adminLogin() {
        return exec(session -> session
                .set("email", ADMIN_EMAIL)
                .set("password", ADMIN_PASSWORD))
                .exec(login())
                .exec(setAuthHeader());
    }

    private ChainBuilder getAdminAnalytics() {
        return exec(http("Get Admin Analytics")
                .get("/api/admin/analytics")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder getAdminApprovals() {
        return exec(http("Get Admin Approvals")
                .get("/api/admin/approvals")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("pendingApprovalIds"))
        );
    }

    private ChainBuilder adminApproveRescue() {
        return doIf(session -> session.contains("pendingApprovalIds") &&
                !((List<?>) session.get("pendingApprovalIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> ids = session.getList("pendingApprovalIds");
                            return session.set("rescueToApprove", ids.get(0));
                        })
                        .exec(http("Admin Approve Rescue Org")
                                .put("/api/admin/approvals/RESCUE_ORG/#{rescueToApprove}/approve")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 204, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder searchUsers() {
        return exec(http("Search Users")
                .get("/api/admin/users/search")
                .header("Authorization", "#{authHeader}")
                .queryParam("page", "1")
                .queryParam("size", "20")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder searchUsersByRole() {
        return exec(http("Search Users by Role")
                .get("/api/admin/users/search")
                .header("Authorization", "#{authHeader}")
                .queryParam("role", "FOSTER")
                .queryParam("page", "1")
                .queryParam("size", "20")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder getContentFlags() {
        return exec(http("Get Content Flags")
                .get("/api/admin/flags")
                .header("Authorization", "#{authHeader}")
                .queryParam("page", "1")
                .queryParam("size", "20")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$.flags[*].id").findAll().optional().saveAs("flagIds"))
        );
    }

    private ChainBuilder getPendingFlags() {
        return exec(http("Get Pending Flags")
                .get("/api/admin/flags")
                .header("Authorization", "#{authHeader}")
                .queryParam("status", "PENDING")
                .queryParam("page", "1")
                .queryParam("size", "20")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder dismissFlag() {
        return doIf(session -> session.contains("flagIds") &&
                !((List<?>) session.get("flagIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> ids = session.getList("flagIds");
                            return session.set("flagToDismiss", ids.get(0));
                        })
                        .exec(http("Dismiss Flag")
                                .put("/api/admin/flags/#{flagToDismiss}/dismiss")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "notes": "Content reviewed, no action needed"
                                        }
                                        """))
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder getAuditLogs() {
        return exec(http("Get Audit Logs")
                .get("/api/admin/audit-logs")
                .header("Authorization", "#{authHeader}")
                .queryParam("page", "1")
                .queryParam("size", "50")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder getUsersForAdminActions() {
        return exec(http("Get Users for Admin Actions")
                .get("/api/admin/users/search")
                .header("Authorization", "#{authHeader}")
                .queryParam("role", "FOSTER")
                .queryParam("status", "ACTIVE")
                .queryParam("page", "1")
                .queryParam("size", "10")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$.users[*].id").findAll().optional().saveAs("activeUserIds"))
        );
    }

    private ChainBuilder suspendUser() {
        return doIf(session -> session.contains("activeUserIds") &&
                !((List<?>) session.get("activeUserIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> ids = session.getList("activeUserIds");
                            return session.set("userToSuspend", ids.get(0));
                        })
                        .exec(http("Admin Suspend User")
                                .put("/api/admin/users/#{userToSuspend}/suspend")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 204, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder reactivateUser() {
        return doIf(session -> session.contains("userToSuspend"))
                .then(
                        exec(http("Admin Reactivate User")
                                .put("/api/admin/users/#{userToSuspend}/reactivate")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 204, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder resetUserPassword() {
        return doIf(session -> session.contains("activeUserIds") &&
                ((List<?>) session.get("activeUserIds")).size() > 1)
                .then(
                        exec(session -> {
                            List<String> ids = session.getList("activeUserIds");
                            return session.set("userToResetPassword", ids.get(1));
                        })
                        .exec(http("Admin Reset User Password")
                                .post("/api/admin/users/#{userToResetPassword}/reset-password")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder approveFlag() {
        return doIf(session -> session.contains("flagIds") &&
                ((List<?>) session.get("flagIds")).size() > 1)
                .then(
                        exec(session -> {
                            List<String> ids = session.getList("flagIds");
                            return session.set("flagToApprove", ids.size() > 1 ? ids.get(1) : ids.get(0));
                        })
                        .exec(http("Admin Approve Flag")
                                .put("/api/admin/flags/#{flagToApprove}/approve")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "notes": "Content violation confirmed, action taken"
                                        }
                                        """))
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    // ==================== THINK TIME ====================

    private ChainBuilder shortPause() {
        return pause(Duration.ofMillis(200), Duration.ofMillis(500));
    }

    private ChainBuilder mediumPause() {
        return pause(Duration.ofMillis(500), Duration.ofMillis(1000));
    }

    // ==================== COMPLETE SCENARIOS ====================

    // Public browsing scenario
    private ScenarioBuilder publicBrowsingScenario = scenario("Public Browsing Flow")
            .exec(browseAvailablePets())
            .exec(shortPause())
            .exec(getFeaturedPets())
            .exec(shortPause())
            .exec(getPlatformStats())
            .exec(shortPause())
            .exec(getPetDetails())
            .exec(shortPause())
            .exec(browseAvailablePetsWithFilters())
            .exec(shortPause())
            .exec(listRescueOrgsPublic())
            .exec(shortPause())
            .exec(getRescueOrgProfile())
            .exec(shortPause())
            .exec(listVetsPublic())
            .exec(shortPause())
            .exec(getVetProfile());

    // Foster complete journey
    private ScenarioBuilder fosterScenario = scenario("Foster Complete Flow")
            .exec(register("FOSTER"))
            .exec(shortPause())
            .exec(login())
            .exec(setAuthHeader())
            .exec(shortPause())
            .exec(completeFosterProfile())
            .exec(shortPause())
            .exec(getProfile())
            .exec(shortPause())
            .exec(listRescueOrgs())
            .exec(session -> {
                if (session.contains("rescueOrgList") && !((List<?>) session.get("rescueOrgList")).isEmpty()) {
                    List<String> orgIds = session.getList("rescueOrgList");
                    return session.set("rescueOrgId", orgIds.get(0));
                }
                return session;
            })
            .exec(shortPause())
            .exec(createPet())
            .exec(shortPause())
            .exec(updatePet())
            .exec(shortPause())
            .exec(getMyPets())
            .exec(shortPause())
            .exec(submitPetForReview())
            .exec(mediumPause())
            .exec(getNotifications())
            .exec(shortPause())
            .exec(getUnreadCount())
            .exec(shortPause())
            .exec(markNotificationRead())
            .exec(shortPause())
            .exec(refreshToken())
            .exec(setAuthHeader())
            .exec(shortPause())
            .exec(getSettings())
            .exec(shortPause())
            .exec(logout());

    // Adopter complete journey
    private ScenarioBuilder adopterScenario = scenario("Adopter Complete Flow")
            .exec(register("ADOPTER"))
            .exec(shortPause())
            .exec(login())
            .exec(setAuthHeader())
            .exec(shortPause())
            .exec(completeAdopterProfile())
            .exec(shortPause())
            .exec(getProfile())
            .exec(shortPause())
            .exec(browseAvailablePets())
            .exec(shortPause())
            .exec(getPetDetails())
            .exec(shortPause())
            .exec(addFavorite())
            .exec(shortPause())
            .exec(getFavorites())
            .exec(shortPause())
            .exec(submitApplication())
            .exec(mediumPause())
            .exec(getMyApplications())
            .exec(shortPause())
            .exec(getMyAdoptions())
            .exec(shortPause())
            .exec(getNotifications())
            .exec(shortPause())
            .exec(getUnreadCount())
            .exec(shortPause())
            .exec(getSettings())
            .exec(shortPause())
            .exec(updateNotificationPreferences())
            .exec(shortPause())
            .exec(refreshToken())
            .exec(setAuthHeader())
            .exec(shortPause())
            .exec(removeFavorite())
            .exec(shortPause())
            .exec(logout());

    // Rescue Org complete journey
    private ScenarioBuilder rescueOrgScenario = scenario("Rescue Org Complete Flow")
            .exec(register("RESCUE_ORG"))
            .exec(shortPause())
            .exec(login())
            .exec(setAuthHeader())
            .exec(shortPause())
            .exec(completeRescueOrgProfile())
            .exec(shortPause())
            .exec(getProfile())
            .exec(shortPause())
            .exec(getPendingPets())
            .exec(shortPause())
            .exec(acceptPet())
            .exec(shortPause())
            .exec(declinePet())
            .exec(shortPause())
            .exec(getPendingVets())
            .exec(shortPause())
            .exec(getApprovedVets())
            .exec(shortPause())
            .exec(approveVet())
            .exec(shortPause())
            .exec(getApplicationsForRescue())
            .exec(shortPause())
            .exec(approveApplication())
            .exec(mediumPause())
            .exec(getNotifications())
            .exec(shortPause())
            .exec(getSettings())
            .exec(shortPause())
            .exec(refreshToken())
            .exec(setAuthHeader())
            .exec(shortPause())
            .exec(logout());

    // Vet complete journey
    private ScenarioBuilder vetScenario = scenario("Vet Complete Flow")
            .exec(register("VET"))
            .exec(shortPause())
            .exec(login())
            .exec(setAuthHeader())
            .exec(shortPause())
            .exec(completeVetProfile())
            .exec(shortPause())
            .exec(getProfile())
            .exec(shortPause())
            .exec(browseAvailablePets())
            .exec(shortPause())
            .exec(getPetDetails())
            .exec(shortPause())
            .exec(lookupPetByMicrochip())
            .exec(shortPause())
            .exec(vetSignOff())
            .exec(mediumPause())
            .exec(getVetSignOffHistory())
            .exec(shortPause())
            .exec(getNotifications())
            .exec(shortPause())
            .exec(getSettings())
            .exec(shortPause())
            .exec(refreshToken())
            .exec(setAuthHeader())
            .exec(shortPause())
            .exec(logout());

    // Admin complete journey
    private ScenarioBuilder adminScenario = scenario("Admin Complete Flow")
            .exec(adminLogin())
            .exec(shortPause())
            .exec(getAdminAnalytics())
            .exec(shortPause())
            .exec(getAdminApprovals())
            .exec(shortPause())
            .exec(adminApproveRescue())
            .exec(shortPause())
            .exec(searchUsers())
            .exec(shortPause())
            .exec(searchUsersByRole())
            .exec(shortPause())
            // User management actions
            .exec(getUsersForAdminActions())
            .exec(shortPause())
            .exec(suspendUser())
            .exec(shortPause())
            .exec(reactivateUser())
            .exec(shortPause())
            .exec(resetUserPassword())
            .exec(shortPause())
            // Content moderation
            .exec(getContentFlags())
            .exec(shortPause())
            .exec(getPendingFlags())
            .exec(shortPause())
            .exec(dismissFlag())
            .exec(shortPause())
            .exec(approveFlag())
            .exec(shortPause())
            .exec(getAuditLogs())
            .exec(mediumPause())
            .exec(getNotifications())
            .exec(shortPause())
            .exec(refreshToken())
            .exec(setAuthHeader())
            .exec(shortPause())
            .exec(logout());

    // ==================== SIMULATION SETUP ====================

    {
        System.out.println("=".repeat(70));
        System.out.println("ALL USER FLOWS SIMULATION");
        System.out.println("=".repeat(70));
        System.out.println("Target URL: " + BASE_URL);
        System.out.println("Users per scenario: " + USERS_PER_SCENARIO);
        System.out.println("Ramp-up duration: " + RAMP_DURATION + "s");
        System.out.println("Test duration: " + TEST_DURATION + "s");
        System.out.println("=".repeat(70));

        setUp(
                publicBrowsingScenario.injectOpen(
                        rampUsers(USERS_PER_SCENARIO * 2).during(Duration.ofSeconds(RAMP_DURATION))
                ).protocols(httpProtocol),
                fosterScenario.injectOpen(
                        rampUsers(USERS_PER_SCENARIO).during(Duration.ofSeconds(RAMP_DURATION))
                ).protocols(httpProtocol),
                adopterScenario.injectOpen(
                        rampUsers(USERS_PER_SCENARIO).during(Duration.ofSeconds(RAMP_DURATION))
                ).protocols(httpProtocol),
                rescueOrgScenario.injectOpen(
                        rampUsers(USERS_PER_SCENARIO).during(Duration.ofSeconds(RAMP_DURATION))
                ).protocols(httpProtocol),
                vetScenario.injectOpen(
                        rampUsers(USERS_PER_SCENARIO).during(Duration.ofSeconds(RAMP_DURATION))
                ).protocols(httpProtocol),
                adminScenario.injectOpen(
                        rampUsers(Math.max(1, USERS_PER_SCENARIO / 2)).during(Duration.ofSeconds(RAMP_DURATION))
                ).protocols(httpProtocol)
        ).maxDuration(Duration.ofSeconds(TEST_DURATION))
                .assertions(
                        global().responseTime().max().lt(10000),        // Max response time < 10s
                        global().responseTime().percentile(95).lt(3000), // 95th percentile < 3s
                        global().successfulRequests().percent().gt(90.0) // >90% success rate
                );
    }
}
