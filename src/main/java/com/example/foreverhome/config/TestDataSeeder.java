package com.example.foreverhome.config;

import com.example.foreverhome.domain.profile.*;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Seeds test accounts when test mode is enabled.
 * Each role gets a dedicated test account with a known password,
 * including fully populated profile records.
 */
@Component
@ConditionalOnProperty(name = "app.test-mode.enabled", havingValue = "true")
public class TestDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestDataSeeder.class);
    private static final String TEST_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FosterRepository fosterRepository;
    private final AdopterRepository adopterRepository;
    private final VetRepository vetRepository;
    private final RescueOrganizationRepository rescueOrgRepository;
    private final JdbcTemplate jdbcTemplate;

    public TestDataSeeder(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          FosterRepository fosterRepository,
                          AdopterRepository adopterRepository,
                          VetRepository vetRepository,
                          RescueOrganizationRepository rescueOrgRepository,
                          JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fosterRepository = fosterRepository;
        this.adopterRepository = adopterRepository;
        this.vetRepository = vetRepository;
        this.rescueOrgRepository = rescueOrgRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        logger.info("Test mode enabled - checking if database is ready for seeding");

        // Check if the database schema is ready before seeding
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'app_users'",
                Integer.class);
            if (tableCount == null || tableCount == 0) {
                logger.warn("Database schema not ready (app_users table does not exist). Skipping test data seeding.");
                return;
            }
        } catch (Exception e) {
            logger.warn("Failed to check database schema readiness: {}. Skipping test data seeding.", e.getMessage());
            return;
        }

        logger.info("Database schema ready - seeding test accounts");

        List<TestAccount> testAccounts = List.of(
            new TestAccount("admin@test.com", "Test Admin", UserRole.ADMIN, null, null),
            new TestAccount("foster@test.com", "Sarah", UserRole.FOSTER, "Sarah", "Mitchell"),
            new TestAccount("foster2@test.com", "James", UserRole.FOSTER, "James", "Rodriguez"),
            new TestAccount("foster3@test.com", "Emily", UserRole.FOSTER, "Emily", "Chen"),
            new TestAccount("foster4@test.com", "Michael", UserRole.FOSTER, "Michael", "Thompson"),
            new TestAccount("foster5@test.com", "Rachel", UserRole.FOSTER, "Rachel", "Anderson"),
            new TestAccount("adopter@test.com", "Test Adopter", UserRole.ADOPTER, null, null),
            new TestAccount("vet@test.com", "Test Vet", UserRole.VET, null, null),
            new TestAccount("rescue@test.com", "Test Rescue Org", UserRole.RESCUE_ORG, null, null)
        );

        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);

        for (TestAccount account : testAccounts) {
            if (!userRepository.existsByEmail(account.email())) {
                User user = User.create(account.email(), encodedPassword, account.role(), account.name());
                user.activate();
                user.markProfileComplete();
                user = userRepository.save(user);

                // Create role-specific profile
                createProfile(user, account);

                logger.info("Created test account with profile: {} ({})", account.email(), account.role());
            } else {
                // User exists, ensure profile exists
                User user = userRepository.findByEmail(account.email()).orElse(null);
                if (user != null) {
                    ensureProfileExists(user, account);
                }
                logger.debug("Test account already exists: {}", account.email());
            }
        }

        logger.info("Test account seeding complete. All accounts use password: {}", TEST_PASSWORD);

        // Seed sample pets
        seedPets();
    }

    private void seedPets() {
        // Check if pets already exist
        Integer petCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pets", Integer.class);
        if (petCount != null && petCount > 0) {
            logger.debug("Pets already exist, skipping pet seeding");
            return;
        }

        // Get the rescue org ID
        UUID rescueOrgId = jdbcTemplate.queryForObject(
            "SELECT r.id FROM rescue_organizations r JOIN app_users u ON r.user_id = u.id WHERE u.email = ?",
            UUID.class, "rescue@test.com");

        if (rescueOrgId == null) {
            logger.warn("Cannot seed pets: rescue org not found");
            return;
        }

        // Get all foster IDs
        UUID foster1Id = getFosterId("foster@test.com");   // Sarah Mitchell
        UUID foster2Id = getFosterId("foster2@test.com");  // James Rodriguez
        UUID foster3Id = getFosterId("foster3@test.com");  // Emily Chen
        UUID foster4Id = getFosterId("foster4@test.com");  // Michael Thompson
        UUID foster5Id = getFosterId("foster5@test.com");  // Rachel Anderson

        logger.info("Seeding sample pets for multiple fosters...");

        // === Sarah Mitchell (foster@test.com) - 1 pet in DRAFT status ===
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Bella", "DOG", "Beagle", 6, "MONTHS", "FEMALE", "MEDIUM",
            "Adorable beagle puppy learning basic commands. Loves treats and belly rubs.",
            "CHIP-DOG-001", "DRAFT", foster1Id, null);

        // === James Rodriguez (foster2@test.com) - 2 pets in PENDING_RESCUE status ===
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Max", "DOG", "Golden Retriever", 3, "YEARS", "MALE", "LARGE",
            "Friendly and energetic golden retriever who loves to play fetch and swim. Great with kids and other dogs.",
            "CHIP-DOG-002", "PENDING_RESCUE", foster2Id, rescueOrgId);

        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Shadow", "CAT", "Black Shorthair", 4, "YEARS", "MALE", "MEDIUM",
            "Mysterious and elegant black cat. Independent but affectionate once he trusts you.",
            "CHIP-CAT-001", "PENDING_RESCUE", foster2Id, rescueOrgId);

        // === Emily Chen (foster3@test.com) - 2 pets in PENDING_VET status ===
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Luna", "CAT", "Siamese", 1, "YEARS", "FEMALE", "SMALL",
            "Beautiful Siamese kitten with bright blue eyes. Playful and affectionate.",
            "CHIP-CAT-002", "PENDING_VET", foster3Id, rescueOrgId);

        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Rocky", "DOG", "German Shepherd Mix", 2, "YEARS", "MALE", "LARGE",
            "Loyal and protective companion. Well-trained and good on leash. Looking for an active family.",
            "CHIP-DOG-003", "PENDING_VET", foster3Id, rescueOrgId);

        // === Michael Thompson (foster4@test.com) - 3 pets in AVAILABLE status ===
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Charlie", "DOG", "Labrador Mix", 2, "YEARS", "MALE", "LARGE",
            "Happy-go-lucky lab mix. Loves everyone and everything. House trained and great with other pets.",
            "CHIP-DOG-004", "AVAILABLE", foster4Id, rescueOrgId);

        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Whiskers", "CAT", "Tabby", 5, "YEARS", "FEMALE", "SMALL",
            "Sweet and gentle tabby cat who loves to curl up in sunny spots. Good with other cats and calm dogs.",
            "CHIP-CAT-003", "AVAILABLE", foster4Id, rescueOrgId);

        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Duke", "DOG", "Boxer", 4, "YEARS", "MALE", "LARGE",
            "Energetic and loyal boxer. Great guard dog but gentle with family. Needs a home with a yard.",
            "CHIP-DOG-005", "AVAILABLE", foster4Id, rescueOrgId);

        // === Rachel Anderson (foster5@test.com) - 2 pets: 1 IN_PROGRESS, 1 ADOPTED ===
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Daisy", "DOG", "Poodle Mix", 3, "YEARS", "FEMALE", "SMALL",
            "Smart and hypoallergenic poodle mix. Currently in adoption process with a wonderful family.",
            "CHIP-DOG-006", "IN_PROGRESS", foster5Id, rescueOrgId);

        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Oliver", "CAT", "Orange Tabby", 2, "YEARS", "MALE", "MEDIUM",
            "Friendly orange tabby who found his forever home. A true success story!",
            "CHIP-CAT-004", "ADOPTED", foster5Id, rescueOrgId);

        logger.info("Created 10 sample pets across 5 fosters with various statuses");
    }

    private UUID getFosterId(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT f.id FROM fosters f JOIN app_users u ON f.user_id = u.id WHERE u.email = ?",
                UUID.class, email);
        } catch (Exception e) {
            logger.warn("Foster not found for email: {}", email);
            return null;
        }
    }

    private void createProfile(User user, TestAccount account) {
        // Use direct SQL inserts to avoid Spring Data JDBC treating new entities as updates
        switch (account.role()) {
            case FOSTER -> {
                UUID id = UUID.randomUUID();
                String firstName = account.firstName() != null ? account.firstName() : "Test";
                String lastName = account.lastName() != null ? account.lastName() : "Foster";
                jdbcTemplate.update("""
                    INSERT INTO fosters (id, user_id, first_name, last_name, phone,
                        address_street, address_city, address_state, address_postal_code, address_country)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id, user.getId(), firstName, lastName, "555-0101",
                    "123 Test Street", "Test City", "TS", "12345", "USA");
            }
            case ADOPTER -> {
                UUID id = UUID.randomUUID();
                jdbcTemplate.update("""
                    INSERT INTO adopters (id, user_id, first_name, last_name, phone, living_situation, pet_experience,
                        address_street, address_city, address_state, address_postal_code, address_country)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id, user.getId(), "Test", "Adopter", "555-0102", "HOUSE", "Experienced pet owner",
                    "123 Test Street", "Test City", "TS", "12345", "USA");
            }
            case VET -> {
                UUID id = UUID.randomUUID();
                jdbcTemplate.update("""
                    INSERT INTO vets (id, user_id, clinic_name, license_number, phone, website, description, verified,
                        address_street, address_city, address_state, address_postal_code, address_country)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id, user.getId(), "Test Veterinary Clinic", "VET-12345", "555-0103",
                    "https://testvet.com", "Test veterinary clinic for development", true,
                    "123 Test Street", "Test City", "TS", "12345", "USA");
            }
            case RESCUE_ORG -> {
                UUID id = UUID.randomUUID();
                jdbcTemplate.update("""
                    INSERT INTO rescue_organizations (id, user_id, name, phone, website, description,
                        contact_name, contact_email, verified, address_street, address_city, address_state, address_postal_code, address_country)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id, user.getId(), "Test Rescue Organization", "555-0104",
                    "https://testrescue.org", "Test rescue organization for development",
                    "Test Contact", "rescue@test.com", true,
                    "123 Test Street", "Test City", "TS", "12345", "USA");
            }
            case ADMIN -> {
                // Admin doesn't need a profile entity
            }
        }
    }

    private void ensureProfileExists(User user, TestAccount account) {
        // Use direct SQL inserts to avoid Spring Data JDBC treating new entities as updates
        switch (account.role()) {
            case FOSTER -> {
                if (fosterRepository.findByUserId(user.getId()).isEmpty()) {
                    UUID id = UUID.randomUUID();
                    String firstName = account.firstName() != null ? account.firstName() : "Test";
                    String lastName = account.lastName() != null ? account.lastName() : "Foster";
                    jdbcTemplate.update("""
                        INSERT INTO fosters (id, user_id, first_name, last_name, phone,
                            address_street, address_city, address_state, address_postal_code, address_country)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        id, user.getId(), firstName, lastName, "555-0101",
                        "123 Test Street", "Test City", "TS", "12345", "USA");
                    logger.info("Created missing Foster profile for: {}", account.email());
                }
            }
            case ADOPTER -> {
                if (adopterRepository.findByUserId(user.getId()).isEmpty()) {
                    UUID id = UUID.randomUUID();
                    jdbcTemplate.update("""
                        INSERT INTO adopters (id, user_id, first_name, last_name, phone, living_situation, pet_experience,
                            address_street, address_city, address_state, address_postal_code, address_country)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        id, user.getId(), "Test", "Adopter", "555-0102", "HOUSE", "Experienced pet owner",
                        "123 Test Street", "Test City", "TS", "12345", "USA");
                    logger.info("Created missing Adopter profile for: {}", account.email());
                }
            }
            case VET -> {
                if (vetRepository.findByUserId(user.getId()).isEmpty()) {
                    UUID id = UUID.randomUUID();
                    jdbcTemplate.update("""
                        INSERT INTO vets (id, user_id, clinic_name, license_number, phone, website, description, verified,
                            address_street, address_city, address_state, address_postal_code, address_country)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        id, user.getId(), "Test Veterinary Clinic", "VET-12345", "555-0103",
                        "https://testvet.com", "Test veterinary clinic for development", true,
                        "123 Test Street", "Test City", "TS", "12345", "USA");
                    logger.info("Created missing Vet profile for: {}", account.email());
                }
            }
            case RESCUE_ORG -> {
                if (rescueOrgRepository.findByUserId(user.getId()).isEmpty()) {
                    UUID id = UUID.randomUUID();
                    jdbcTemplate.update("""
                        INSERT INTO rescue_organizations (id, user_id, name, phone, website, description,
                            contact_name, contact_email, verified, address_street, address_city, address_state, address_postal_code, address_country)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        id, user.getId(), "Test Rescue Organization", "555-0104",
                        "https://testrescue.org", "Test rescue organization for development",
                        "Test Contact", "rescue@test.com", true,
                        "123 Test Street", "Test City", "TS", "12345", "USA");
                    logger.info("Created missing RescueOrganization profile for: {}", account.email());
                }
            }
            case ADMIN -> {
                // Admin doesn't need a profile entity
            }
        }
    }

    private record TestAccount(String email, String name, UserRole role, String firstName, String lastName) {}
}
