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
            new TestAccount("admin@test.com", "Test Admin", UserRole.ADMIN),
            new TestAccount("foster@test.com", "Test Foster", UserRole.FOSTER),
            new TestAccount("adopter@test.com", "Test Adopter", UserRole.ADOPTER),
            new TestAccount("vet@test.com", "Test Vet", UserRole.VET),
            new TestAccount("rescue@test.com", "Test Rescue Org", UserRole.RESCUE_ORG)
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

        // Get the foster and rescue org IDs
        UUID fosterId = jdbcTemplate.queryForObject(
            "SELECT f.id FROM fosters f JOIN app_users u ON f.user_id = u.id WHERE u.email = ?",
            UUID.class, "foster@test.com");
        UUID rescueOrgId = jdbcTemplate.queryForObject(
            "SELECT r.id FROM rescue_organizations r JOIN app_users u ON r.user_id = u.id WHERE u.email = ?",
            UUID.class, "rescue@test.com");

        if (fosterId == null || rescueOrgId == null) {
            logger.warn("Cannot seed pets: foster or rescue org not found");
            return;
        }

        logger.info("Seeding sample pets...");

        // Pet 1: Available dog (linked to rescue, vet signed off)
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Max", "DOG", "Golden Retriever", 3, "YEARS", "MALE", "LARGE",
            "Friendly and energetic golden retriever who loves to play fetch and swim. Great with kids and other dogs.",
            "CHIP-DOG-001", "AVAILABLE", fosterId, rescueOrgId);

        // Pet 2: Available cat (linked to rescue)
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Whiskers", "CAT", "Tabby", 2, "YEARS", "FEMALE", "SMALL",
            "Sweet and gentle tabby cat who loves to curl up in sunny spots. Good with other cats.",
            "CHIP-CAT-001", "AVAILABLE", fosterId, rescueOrgId);

        // Pet 3: Pending vet sign-off (linked to rescue, awaiting vet)
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Luna", "CAT", "Siamese", 1, "YEARS", "FEMALE", "SMALL",
            "Beautiful Siamese kitten with bright blue eyes. Playful and affectionate.",
            "CHIP-CAT-002", "PENDING_VET", fosterId, rescueOrgId);

        // Pet 4: Pending rescue review (not yet accepted by rescue)
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Rocky", "DOG", "German Shepherd Mix", 4, "YEARS", "MALE", "LARGE",
            "Loyal and protective companion. Well-trained and good on leash.",
            "CHIP-DOG-002", "PENDING_RESCUE", fosterId, rescueOrgId);

        // Pet 5: Draft (not submitted yet)
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Bella", "DOG", "Beagle", 6, "MONTHS", "FEMALE", "MEDIUM",
            "Adorable beagle puppy learning basic commands. Loves treats and belly rubs.",
            "CHIP-DOG-003", "DRAFT", fosterId, null);

        // Pet 6: Available dog for adoption applications
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Charlie", "DOG", "Labrador Mix", 2, "YEARS", "MALE", "LARGE",
            "Happy-go-lucky lab mix. Loves everyone and everything. House trained.",
            "CHIP-DOG-004", "AVAILABLE", fosterId, rescueOrgId);

        // Pet 7: Available cat
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Oliver", "CAT", "Orange Tabby", 5, "YEARS", "MALE", "MEDIUM",
            "Relaxed senior cat looking for a quiet home. Lap cat who loves chin scratches.",
            "CHIP-CAT-003", "AVAILABLE", fosterId, rescueOrgId);

        // Pet 8: In-progress adoption
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            UUID.randomUUID(), "Daisy", "DOG", "Poodle Mix", 3, "YEARS", "FEMALE", "SMALL",
            "Smart and hypoallergenic poodle mix. Currently in adoption process.",
            "CHIP-DOG-005", "IN_PROGRESS", fosterId, rescueOrgId);

        logger.info("Created 8 sample pets with various statuses");
    }

    private void createProfile(User user, TestAccount account) {
        // Use direct SQL inserts to avoid Spring Data JDBC treating new entities as updates
        switch (account.role()) {
            case FOSTER -> {
                UUID id = UUID.randomUUID();
                jdbcTemplate.update("""
                    INSERT INTO fosters (id, user_id, first_name, last_name, phone,
                        address_street, address_city, address_state, address_postal_code, address_country)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id, user.getId(), "Test", "Foster", "555-0101",
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
                    jdbcTemplate.update("""
                        INSERT INTO fosters (id, user_id, first_name, last_name, phone,
                            address_street, address_city, address_state, address_postal_code, address_country)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        id, user.getId(), "Test", "Foster", "555-0101",
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

    private record TestAccount(String email, String name, UserRole role) {}
}
