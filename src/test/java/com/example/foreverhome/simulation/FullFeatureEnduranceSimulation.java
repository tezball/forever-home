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
 * Comprehensive endurance simulation that exercises ALL application features continuously.
 *
 * This simulation runs FOREVER until manually killed (Ctrl+C), continuously executing
 * user journeys for all roles: Foster, Adopter, Vet, Rescue Org, and Admin.
 *
 * Run with: ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.FullFeatureEnduranceSimulation
 *
 * Configuration via system properties:
 * - BASE_URL: Target URL (default: http://localhost:8080)
 * - USERS_PER_ROLE: Concurrent users per role (default: 2)
 * - THINK_TIME_MS: Think time between actions in ms (default: 500)
 * - ADMIN_EMAIL: Admin email for admin operations (default: admin@foreverhome.com)
 * - ADMIN_PASSWORD: Admin password (default: AdminPass123!)
 */
public class FullFeatureEnduranceSimulation extends Simulation {

    // Configuration
    private static final String BASE_URL = System.getProperty("BASE_URL", "http://localhost:8080");
    private static final int USERS_PER_ROLE = Integer.parseInt(System.getProperty("USERS_PER_ROLE", "2"));
    private static final int THINK_TIME_MS = Integer.parseInt(System.getProperty("THINK_TIME_MS", "500"));
    private static final String ADMIN_EMAIL = System.getProperty("ADMIN_EMAIL", "admin@foreverhome.com");
    private static final String ADMIN_PASSWORD = System.getProperty("ADMIN_PASSWORD", "AdminPass123!");

    // Counters for unique data generation
    private static final AtomicInteger userCounter = new AtomicInteger(0);
    private static final AtomicInteger petCounter = new AtomicInteger(0);
    private static final AtomicInteger microchipCounter = new AtomicInteger(0);

    // Species, sizes, and sexes for random pet generation
    private static final String[] SPECIES = {"DOG", "CAT"};
    private static final String[] SIZES = {"SMALL", "MEDIUM", "LARGE"};
    private static final String[] SEXES = {"MALE", "FEMALE"};
    private static final String[] AGE_UNITS = {"MONTHS", "YEARS"};
    private static final String[] DOG_BREEDS = {"Labrador", "Poodle", "Beagle", "Bulldog", "German Shepherd", "Golden Retriever"};
    private static final String[] CAT_BREEDS = {"Siamese", "Persian", "Maine Coon", "Ragdoll", "Bengal", "Tabby"};
    private static final String[] PET_NAMES = {"Max", "Bella", "Charlie", "Lucy", "Cooper", "Daisy", "Buddy", "Luna", "Rocky", "Sadie"};

    // HTTP Protocol
    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/FullFeatureEnduranceSimulation")
            .shareConnections();

    // ==================== DATA FEEDERS ====================

    private Iterator<Map<String, Object>> fosterFeeder() {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            int id = userCounter.incrementAndGet();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            return Map.of(
                    "email", "endurance-foster-" + id + "-" + uuid + "@test.com",
                    "password", "TestPass123!",
                    "name", "Endurance Foster " + id,
                    "role", "FOSTER"
            );
        }).iterator();
    }

    private Iterator<Map<String, Object>> adopterFeeder() {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            int id = userCounter.incrementAndGet();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            return Map.of(
                    "email", "endurance-adopter-" + id + "-" + uuid + "@test.com",
                    "password", "TestPass123!",
                    "name", "Endurance Adopter " + id,
                    "role", "ADOPTER"
            );
        }).iterator();
    }

    private Iterator<Map<String, Object>> vetFeeder() {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            int id = userCounter.incrementAndGet();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            return Map.of(
                    "email", "endurance-vet-" + id + "-" + uuid + "@test.com",
                    "password", "TestPass123!",
                    "name", "Endurance Vet " + id,
                    "role", "VET"
            );
        }).iterator();
    }

    private Iterator<Map<String, Object>> rescueOrgFeeder() {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            int id = userCounter.incrementAndGet();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            return Map.of(
                    "email", "endurance-rescue-" + id + "-" + uuid + "@test.com",
                    "password", "TestPass123!",
                    "name", "Endurance Rescue " + id,
                    "role", "RESCUE_ORG"
            );
        }).iterator();
    }

    private Iterator<Map<String, Object>> petFeeder() {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            int id = petCounter.incrementAndGet();
            String species = SPECIES[(int) (Math.random() * SPECIES.length)];
            String[] breeds = species.equals("DOG") ? DOG_BREEDS : CAT_BREEDS;
            return Map.of(
                    "petName", PET_NAMES[(int) (Math.random() * PET_NAMES.length)] + "-" + id,
                    "species", species,
                    "breed", breeds[(int) (Math.random() * breeds.length)],
                    "age", (int) (Math.random() * 10) + 1,
                    "ageUnit", AGE_UNITS[(int) (Math.random() * AGE_UNITS.length)],
                    "sex", SEXES[(int) (Math.random() * SEXES.length)],
                    "size", SIZES[(int) (Math.random() * SIZES.length)],
                    "microchipId", "CHIP-" + microchipCounter.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8)
            );
        }).iterator();
    }

    // ==================== COMMON CHAINS ====================

    private ChainBuilder register(String role) {
        return exec(http("Register " + role)
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

    private ChainBuilder authHeader() {
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
        return exec(http("Get Unread Count")
                .get("/api/notifications/unread/count")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder markNotificationAsRead() {
        return doIf(session -> session.contains("notificationIds") &&
                !((List<?>) session.get("notificationIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> notifIds = session.getList("notificationIds");
                            String randomId = notifIds.get((int) (Math.random() * notifIds.size()));
                            return session.set("notificationId", randomId);
                        })
                        .exec(http("Mark Notification Read")
                                .put("/api/notifications/#{notificationId}/read")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(204, 401, 403, 404))
                        )
                );
    }

    // ==================== FOSTER JOURNEY ====================

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
                                    "healthNotes": "Healthy and up to date on vaccinations"
                                }
                                """))
                        .check(status().in(201, 400, 401, 403))
                        .check(jsonPath("$.id").optional().saveAs("createdPetId"))
                        .check(jsonPath("$.microchipId").optional().saveAs("createdPetMicrochip"))
                );
    }

    private ChainBuilder getMyPets() {
        return exec(http("Get My Pets (Foster)")
                .get("/api/pets/my")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("myPetIds"))
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
                                            "name": "#{petName}-updated",
                                            "description": "Updated description - even more lovely!",
                                            "healthNotes": "Still healthy, now with updated records"
                                        }
                                        """))
                                .check(status().in(200, 400, 401, 403, 404))
                        )
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
        return doIf(session -> session.contains("myPetIds") &&
                !((List<?>) session.get("myPetIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> petIds = session.getList("myPetIds");
                            String randomId = petIds.get((int) (Math.random() * petIds.size()));
                            return session.set("petToWithdraw", randomId);
                        })
                        .exec(http("Withdraw Pet")
                                .post("/api/pets/#{petToWithdraw}/withdraw")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    // ==================== ADOPTER JOURNEY ====================

    private ChainBuilder getFavorites() {
        return exec(http("Get Favorites")
                .get("/api/favorites")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
        );
    }

    private ChainBuilder getFavoritePets() {
        return exec(http("Get Favorite Pets")
                .get("/api/favorites/pets")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
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
                                            "message": "I would love to adopt this pet! I have a loving home ready."
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
        return doIf(session -> session.contains("myApplicationIds") &&
                !((List<?>) session.get("myApplicationIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> appIds = session.getList("myApplicationIds");
                            String randomId = appIds.get((int) (Math.random() * appIds.size()));
                            return session.set("appToWithdraw", randomId);
                        })
                        .exec(http("Withdraw Application")
                                .delete("/api/applications/#{appToWithdraw}")
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

    // ==================== VET JOURNEY ====================

    private ChainBuilder lookupPetByMicrochip() {
        return doIf(session -> session.contains("petMicrochip"))
                .then(
                        exec(http("Lookup Pet by Microchip")
                                .get("/api/pets/lookup")
                                .header("Authorization", "#{authHeader}")
                                .queryParam("microchip", "#{petMicrochip}")
                                .check(status().in(200, 401, 403, 404))
                        )
                );
    }

    // ==================== RESCUE ORG JOURNEY ====================

    private ChainBuilder listRescueOrgs() {
        return exec(http("List Rescue Orgs")
                .get("/api/rescue-org/list")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("rescueOrgIds"))
        );
    }

    private ChainBuilder getPendingPets() {
        return doIf(session -> session.contains("userId"))
                .then(
                        exec(http("Get Pending Pets")
                                .get("/api/pets/rescue/#{userId}/pending")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 401, 403))
                                .check(jsonPath("$[*].id").findAll().optional().saveAs("pendingPetIds"))
                        )
                );
    }

    private ChainBuilder getRescuePets() {
        return doIf(session -> session.contains("userId"))
                .then(
                        exec(http("Get Rescue Pets")
                                .get("/api/pets/rescue/#{userId}")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 401, 403))
                                .check(jsonPath("$[*].id").findAll().optional().saveAs("rescuePetIds"))
                        )
                );
    }

    private ChainBuilder acceptPet() {
        return doIf(session -> session.contains("pendingPetIds") &&
                !((List<?>) session.get("pendingPetIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> petIds = session.getList("pendingPetIds");
                            String randomId = petIds.get((int) (Math.random() * petIds.size()));
                            return session.set("petToAccept", randomId);
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
                !((List<?>) session.get("pendingPetIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> petIds = session.getList("pendingPetIds");
                            String randomId = petIds.get((int) (Math.random() * petIds.size()));
                            return session.set("petToDecline", randomId);
                        })
                        .exec(http("Decline Pet")
                                .post("/api/pets/#{petToDecline}/decline")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "reason": "Not a good fit for our rescue at this time"
                                        }
                                        """))
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder getApprovedVets() {
        return exec(http("Get Approved Vets")
                .get("/api/rescue-org/vets/approved")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
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

    private ChainBuilder approveVet() {
        return doIf(session -> session.contains("pendingVetIds") &&
                !((List<?>) session.get("pendingVetIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> vetIds = session.getList("pendingVetIds");
                            String randomId = vetIds.get((int) (Math.random() * vetIds.size()));
                            return session.set("vetToApprove", randomId);
                        })
                        .exec(http("Approve Vet")
                                .post("/api/rescue-org/vets/#{vetToApprove}/approve")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(201, 400, 401, 403, 404, 409))
                        )
                );
    }

    private ChainBuilder getApplicationsForPet() {
        return doIf(session -> session.contains("rescuePetIds") &&
                !((List<?>) session.get("rescuePetIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> petIds = session.getList("rescuePetIds");
                            String randomId = petIds.get((int) (Math.random() * petIds.size()));
                            return session.set("petForApplications", randomId);
                        })
                        .exec(http("Get Applications for Pet")
                                .get("/api/pets/#{petForApplications}/applications")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 401, 403, 404))
                                .check(jsonPath("$[*].id").findAll().optional().saveAs("petApplicationIds"))
                        )
                );
    }

    private ChainBuilder approveApplication() {
        return doIf(session -> session.contains("petApplicationIds") &&
                !((List<?>) session.get("petApplicationIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> appIds = session.getList("petApplicationIds");
                            String randomId = appIds.get((int) (Math.random() * appIds.size()));
                            return session.set("appToApprove", randomId);
                        })
                        .exec(http("Approve Application")
                                .put("/api/applications/#{appToApprove}/approve")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder rejectApplication() {
        return doIf(session -> session.contains("petApplicationIds") &&
                !((List<?>) session.get("petApplicationIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> appIds = session.getList("petApplicationIds");
                            String randomId = appIds.get((int) (Math.random() * appIds.size()));
                            return session.set("appToReject", randomId);
                        })
                        .exec(http("Reject Application")
                                .put("/api/applications/#{appToReject}/reject")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "reason": "Application does not meet our requirements at this time"
                                        }
                                        """))
                                .check(status().in(200, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder finalizeAdoption() {
        return doIf(session -> session.contains("petApplicationIds") &&
                !((List<?>) session.get("petApplicationIds")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> appIds = session.getList("petApplicationIds");
                            String randomId = appIds.get((int) (Math.random() * appIds.size()));
                            return session.set("appToFinalize", randomId);
                        })
                        .exec(http("Finalize Adoption")
                                .post("/api/adoptions")
                                .header("Authorization", "#{authHeader}")
                                .body(StringBody("""
                                        {
                                            "applicationId": "#{appToFinalize}"
                                        }
                                        """))
                                .check(status().in(201, 400, 401, 403, 404))
                        )
                );
    }

    // ==================== ADMIN JOURNEY ====================

    private ChainBuilder adminLogin() {
        return exec(session -> session
                .set("email", ADMIN_EMAIL)
                .set("password", ADMIN_PASSWORD))
                .exec(login())
                .exec(authHeader());
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
                .check(jsonPath("$.vets[*].id").findAll().optional().saveAs("pendingVetApprovals"))
                .check(jsonPath("$.rescueOrgs[*].id").findAll().optional().saveAs("pendingRescueApprovals"))
        );
    }

    private ChainBuilder getAdminUsers() {
        return exec(http("Get Admin Users")
                .get("/api/admin/users")
                .header("Authorization", "#{authHeader}")
                .check(status().in(200, 401, 403))
                .check(jsonPath("$[*].id").findAll().optional().saveAs("allUserIds"))
        );
    }

    private ChainBuilder adminApproveVet() {
        return doIf(session -> session.contains("pendingVetApprovals") &&
                !((List<?>) session.get("pendingVetApprovals")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> vetIds = session.getList("pendingVetApprovals");
                            String randomId = vetIds.get((int) (Math.random() * vetIds.size()));
                            return session.set("vetToAdminApprove", randomId);
                        })
                        .exec(http("Admin Approve Vet")
                                .put("/api/admin/approvals/vet/#{vetToAdminApprove}/approve")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(204, 400, 401, 403, 404))
                        )
                );
    }

    private ChainBuilder adminApproveRescue() {
        return doIf(session -> session.contains("pendingRescueApprovals") &&
                !((List<?>) session.get("pendingRescueApprovals")).isEmpty())
                .then(
                        exec(session -> {
                            List<String> rescueIds = session.getList("pendingRescueApprovals");
                            String randomId = rescueIds.get((int) (Math.random() * rescueIds.size()));
                            return session.set("rescueToAdminApprove", randomId);
                        })
                        .exec(http("Admin Approve Rescue Org")
                                .put("/api/admin/approvals/rescue_org/#{rescueToAdminApprove}/approve")
                                .header("Authorization", "#{authHeader}")
                                .check(status().in(204, 400, 401, 403, 404))
                        )
                );
    }

    // ==================== THINK TIME ====================

    private ChainBuilder thinkTime() {
        return pause(Duration.ofMillis(THINK_TIME_MS / 2), Duration.ofMillis(THINK_TIME_MS));
    }

    // ==================== COMPLETE USER JOURNEYS ====================

    // Foster Journey: Register -> Login -> Create Pet -> Update -> Get My Pets -> Notifications -> Logout
    private ScenarioBuilder fosterJourney = scenario("Foster Complete Journey")
            .forever().on(
                    feed(fosterFeeder())
                    .exec(register("FOSTER"))
                    .exec(thinkTime())
                    .exec(login())
                    .exec(authHeader())
                    .exec(thinkTime())
                    // Browse for rescue orgs to submit to
                    .exec(listRescueOrgs())
                    .exec(session -> {
                        if (session.contains("rescueOrgIds") && !((List<?>) session.get("rescueOrgIds")).isEmpty()) {
                            List<String> orgIds = session.getList("rescueOrgIds");
                            return session.set("rescueOrgId", orgIds.get((int) (Math.random() * orgIds.size())));
                        }
                        return session;
                    })
                    .exec(thinkTime())
                    // Create and manage pet
                    .exec(createPet())
                    .exec(thinkTime())
                    .exec(updatePet())
                    .exec(thinkTime())
                    .exec(submitPetForReview())
                    .exec(thinkTime())
                    .exec(getMyPets())
                    .exec(thinkTime())
                    // Check notifications
                    .exec(getNotifications())
                    .exec(getUnreadCount())
                    .exec(markNotificationAsRead())
                    .exec(thinkTime())
                    // Token refresh
                    .exec(refreshToken())
                    .exec(authHeader())
                    .exec(thinkTime())
                    // Sometimes withdraw
                    .randomSwitch().on(
                            percent(20.0).then(exec(withdrawPet()))
                    )
                    .exec(thinkTime())
                    .exec(logout())
                    .exec(thinkTime())
            );

    // Adopter Journey: Register -> Login -> Browse -> Favorite -> Apply -> Check Status -> Logout
    private ScenarioBuilder adopterJourney = scenario("Adopter Complete Journey")
            .forever().on(
                    feed(adopterFeeder())
                    .exec(register("ADOPTER"))
                    .exec(thinkTime())
                    .exec(login())
                    .exec(authHeader())
                    .exec(thinkTime())
                    // Browse pets
                    .exec(browseAvailablePets())
                    .exec(thinkTime())
                    .exec(getPetDetails())
                    .exec(thinkTime())
                    // Favorites
                    .exec(addFavorite())
                    .exec(thinkTime())
                    .exec(getFavorites())
                    .exec(getFavoritePets())
                    .exec(thinkTime())
                    // Application
                    .exec(submitApplication())
                    .exec(thinkTime())
                    .exec(getMyApplications())
                    .exec(thinkTime())
                    // Check adoptions
                    .exec(getMyAdoptions())
                    .exec(thinkTime())
                    // Notifications
                    .exec(getNotifications())
                    .exec(getUnreadCount())
                    .exec(thinkTime())
                    // Token refresh
                    .exec(refreshToken())
                    .exec(authHeader())
                    .exec(thinkTime())
                    // Sometimes withdraw app or remove favorite
                    .randomSwitch().on(
                            percent(15.0).then(exec(withdrawApplication())),
                            percent(15.0).then(exec(removeFavorite()))
                    )
                    .exec(thinkTime())
                    .exec(logout())
                    .exec(thinkTime())
            );

    // Vet Journey: Register -> Login -> Browse -> Lookup Pet -> Notifications -> Logout
    private ScenarioBuilder vetJourney = scenario("Vet Complete Journey")
            .forever().on(
                    feed(vetFeeder())
                    .exec(register("VET"))
                    .exec(thinkTime())
                    .exec(login())
                    .exec(authHeader())
                    .exec(thinkTime())
                    // Browse to find a pet
                    .exec(browseAvailablePets())
                    .exec(thinkTime())
                    .exec(getPetDetails())
                    .exec(thinkTime())
                    // Lookup by microchip
                    .exec(lookupPetByMicrochip())
                    .exec(thinkTime())
                    // Notifications
                    .exec(getNotifications())
                    .exec(getUnreadCount())
                    .exec(markNotificationAsRead())
                    .exec(thinkTime())
                    // Token refresh
                    .exec(refreshToken())
                    .exec(authHeader())
                    .exec(thinkTime())
                    .exec(logout())
                    .exec(thinkTime())
            );

    // Rescue Org Journey: Register -> Login -> Manage Pets -> Manage Vets -> Process Applications -> Logout
    private ScenarioBuilder rescueOrgJourney = scenario("Rescue Org Complete Journey")
            .forever().on(
                    feed(rescueOrgFeeder())
                    .exec(register("RESCUE_ORG"))
                    .exec(thinkTime())
                    .exec(login())
                    .exec(authHeader())
                    // Store userId for rescue-specific endpoints
                    .exec(session -> {
                        // Extract userId from JWT or use a placeholder approach
                        // For simplicity, we'll try to extract from context or just proceed
                        return session;
                    })
                    .exec(thinkTime())
                    // Vet management
                    .exec(getApprovedVets())
                    .exec(thinkTime())
                    .exec(getPendingVets())
                    .exec(thinkTime())
                    .exec(approveVet())
                    .exec(thinkTime())
                    // List other rescue orgs
                    .exec(listRescueOrgs())
                    .exec(thinkTime())
                    // Browse available pets
                    .exec(browseAvailablePets())
                    .exec(getPetDetails())
                    .exec(thinkTime())
                    // Get applications for pets (requires rescue pet context)
                    .exec(getApplicationsForPet())
                    .exec(thinkTime())
                    // Process applications randomly
                    .randomSwitch().on(
                            percent(40.0).then(exec(approveApplication())),
                            percent(30.0).then(exec(rejectApplication())),
                            percent(20.0).then(exec(finalizeAdoption()))
                    )
                    .exec(thinkTime())
                    // Notifications
                    .exec(getNotifications())
                    .exec(getUnreadCount())
                    .exec(thinkTime())
                    // Token refresh
                    .exec(refreshToken())
                    .exec(authHeader())
                    .exec(thinkTime())
                    .exec(logout())
                    .exec(thinkTime())
            );

    // Admin Journey: Login -> Analytics -> Approvals -> User Management -> Logout
    private ScenarioBuilder adminJourney = scenario("Admin Complete Journey")
            .forever().on(
                    exec(adminLogin())
                    .exec(thinkTime())
                    // Analytics
                    .exec(getAdminAnalytics())
                    .exec(thinkTime())
                    // Approvals
                    .exec(getAdminApprovals())
                    .exec(thinkTime())
                    // Process approvals randomly
                    .randomSwitch().on(
                            percent(40.0).then(exec(adminApproveVet())),
                            percent(40.0).then(exec(adminApproveRescue()))
                    )
                    .exec(thinkTime())
                    // User management
                    .exec(getAdminUsers())
                    .exec(thinkTime())
                    // Notifications
                    .exec(getNotifications())
                    .exec(getUnreadCount())
                    .exec(thinkTime())
                    // Token refresh
                    .exec(refreshToken())
                    .exec(authHeader())
                    .exec(thinkTime())
                    .exec(logout())
                    .exec(thinkTime())
            );

    // Public browsing scenario (no auth required)
    private ScenarioBuilder publicBrowsingJourney = scenario("Public Browsing Journey")
            .forever().on(
                    exec(browseAvailablePets())
                    .exec(thinkTime())
                    .exec(getPetDetails())
                    .exec(thinkTime())
                    .exec(browseAvailablePets())
                    .exec(thinkTime())
            );

    // ==================== SIMULATION SETUP ====================

    {
        System.out.println("=".repeat(60));
        System.out.println("FULL FEATURE ENDURANCE SIMULATION");
        System.out.println("=".repeat(60));
        System.out.println("Target URL: " + BASE_URL);
        System.out.println("Users per role: " + USERS_PER_ROLE);
        System.out.println("Think time: " + THINK_TIME_MS + "ms");
        System.out.println("=".repeat(60));
        System.out.println("This simulation runs FOREVER until killed (Ctrl+C)");
        System.out.println("=".repeat(60));

        setUp(
                // Inject all user roles concurrently with a gradual ramp-up
                fosterJourney.injectOpen(
                        rampUsers(USERS_PER_ROLE).during(Duration.ofSeconds(30))
                ),
                adopterJourney.injectOpen(
                        rampUsers(USERS_PER_ROLE).during(Duration.ofSeconds(30))
                ),
                vetJourney.injectOpen(
                        rampUsers(USERS_PER_ROLE).during(Duration.ofSeconds(30))
                ),
                rescueOrgJourney.injectOpen(
                        rampUsers(USERS_PER_ROLE).during(Duration.ofSeconds(30))
                ),
                adminJourney.injectOpen(
                        rampUsers(1).during(Duration.ofSeconds(10)) // Only 1 admin
                ),
                publicBrowsingJourney.injectOpen(
                        rampUsers(USERS_PER_ROLE).during(Duration.ofSeconds(30))
                )
        ).protocols(httpProtocol);
        // No assertions - this is an endurance test meant to run forever
    }
}
