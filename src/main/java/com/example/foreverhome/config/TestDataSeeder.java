package com.example.foreverhome.config;

import com.example.foreverhome.domain.profile.*;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.repository.*;
import com.example.foreverhome.service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

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
    private final S3StorageService storageService;
    private final ResourceLoader resourceLoader;

    public TestDataSeeder(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          FosterRepository fosterRepository,
                          AdopterRepository adopterRepository,
                          VetRepository vetRepository,
                          RescueOrganizationRepository rescueOrgRepository,
                          JdbcTemplate jdbcTemplate,
                          S3StorageService storageService,
                          ResourceLoader resourceLoader) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fosterRepository = fosterRepository;
        this.adopterRepository = adopterRepository;
        this.vetRepository = vetRepository;
        this.rescueOrgRepository = rescueOrgRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
        this.resourceLoader = resourceLoader;
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

        // Seed sample pets with images
        seedPets();

        // Seed a vet approval request (if not already exists)
        seedVetApprovalRequests();
    }

    private void seedPets() {
        // Check if pets already exist
        Integer petCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pets", Integer.class);
        if (petCount != null && petCount > 0) {
            logger.debug("Pets already exist, skipping pet seeding");
            // Still check if images need to be seeded
            seedPetImagesIfNeeded();
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

        // Store pet IDs for image association
        List<PetSeed> petSeeds = new ArrayList<>();

        // === Sarah Mitchell (foster@test.com) - 1 pet in DRAFT status ===
        UUID bellaId = UUID.randomUUID();
        String bellaMicrochip = "CHIP-DOG-001";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            bellaId, "Bella", "DOG", "Beagle", 6, "MONTHS", "FEMALE", "MEDIUM",
            "Adorable beagle puppy learning basic commands. Loves treats and belly rubs.",
            bellaMicrochip, "DRAFT", foster1Id, null);
        petSeeds.add(new PetSeed(bellaId, bellaMicrochip, "DOG", "dog-1.jpg"));

        // === James Rodriguez (foster2@test.com) - 2 pets in PENDING_RESCUE status ===
        UUID maxId = UUID.randomUUID();
        String maxMicrochip = "CHIP-DOG-002";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            maxId, "Max", "DOG", "Golden Retriever", 3, "YEARS", "MALE", "LARGE",
            "Friendly and energetic golden retriever who loves to play fetch and swim. Great with kids and other dogs.",
            maxMicrochip, "PENDING_RESCUE", foster2Id, rescueOrgId);
        petSeeds.add(new PetSeed(maxId, maxMicrochip, "DOG", "dog-2.jpg"));

        UUID shadowId = UUID.randomUUID();
        String shadowMicrochip = "CHIP-CAT-001";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            shadowId, "Shadow", "CAT", "Black Shorthair", 4, "YEARS", "MALE", "MEDIUM",
            "Mysterious and elegant black cat. Independent but affectionate once he trusts you.",
            shadowMicrochip, "PENDING_RESCUE", foster2Id, rescueOrgId);
        petSeeds.add(new PetSeed(shadowId, shadowMicrochip, "CAT", "cat-1.jpg"));

        // === Emily Chen (foster3@test.com) - 2 pets in PENDING_VET status ===
        UUID lunaId = UUID.randomUUID();
        String lunaMicrochip = "CHIP-CAT-002";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            lunaId, "Luna", "CAT", "Siamese", 1, "YEARS", "FEMALE", "SMALL",
            "Beautiful Siamese kitten with bright blue eyes. Playful and affectionate.",
            lunaMicrochip, "PENDING_VET", foster3Id, rescueOrgId);
        petSeeds.add(new PetSeed(lunaId, lunaMicrochip, "CAT", "cat-2.jpg"));

        UUID rockyId = UUID.randomUUID();
        String rockyMicrochip = "CHIP-DOG-003";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            rockyId, "Rocky", "DOG", "German Shepherd Mix", 2, "YEARS", "MALE", "LARGE",
            "Loyal and protective companion. Well-trained and good on leash. Looking for an active family.",
            rockyMicrochip, "PENDING_VET", foster3Id, rescueOrgId);
        petSeeds.add(new PetSeed(rockyId, rockyMicrochip, "DOG", "dog-3.jpg"));

        // === Michael Thompson (foster4@test.com) - 3 pets in AVAILABLE status ===
        UUID charlieId = UUID.randomUUID();
        String charlieMicrochip = "CHIP-DOG-004";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            charlieId, "Charlie", "DOG", "Labrador Mix", 2, "YEARS", "MALE", "LARGE",
            "Happy-go-lucky lab mix. Loves everyone and everything. House trained and great with other pets.",
            charlieMicrochip, "AVAILABLE", foster4Id, rescueOrgId);
        petSeeds.add(new PetSeed(charlieId, charlieMicrochip, "DOG", "dog-4.jpg"));

        UUID whiskersId = UUID.randomUUID();
        String whiskersMicrochip = "CHIP-CAT-003";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            whiskersId, "Whiskers", "CAT", "Tabby", 5, "YEARS", "FEMALE", "SMALL",
            "Sweet and gentle tabby cat who loves to curl up in sunny spots. Good with other cats and calm dogs.",
            whiskersMicrochip, "AVAILABLE", foster4Id, rescueOrgId);
        petSeeds.add(new PetSeed(whiskersId, whiskersMicrochip, "CAT", "cat-3.jpg"));

        UUID dukeId = UUID.randomUUID();
        String dukeMicrochip = "CHIP-DOG-005";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            dukeId, "Duke", "DOG", "Boxer", 4, "YEARS", "MALE", "LARGE",
            "Energetic and loyal boxer. Great guard dog but gentle with family. Needs a home with a yard.",
            dukeMicrochip, "AVAILABLE", foster4Id, rescueOrgId);
        petSeeds.add(new PetSeed(dukeId, dukeMicrochip, "DOG", "dog-5.jpg"));

        // === Rachel Anderson (foster5@test.com) - 2 pets: 1 IN_PROGRESS, 1 ADOPTED ===
        UUID daisyId = UUID.randomUUID();
        String daisyMicrochip = "CHIP-DOG-006";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            daisyId, "Daisy", "DOG", "Poodle Mix", 3, "YEARS", "FEMALE", "SMALL",
            "Smart and hypoallergenic poodle mix. Currently in adoption process with a wonderful family.",
            daisyMicrochip, "IN_PROGRESS", foster5Id, rescueOrgId);
        petSeeds.add(new PetSeed(daisyId, daisyMicrochip, "DOG", "dog-6.jpg"));

        UUID oliverId = UUID.randomUUID();
        String oliverMicrochip = "CHIP-CAT-004";
        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            oliverId, "Oliver", "CAT", "Orange Tabby", 2, "YEARS", "MALE", "MEDIUM",
            "Friendly orange tabby who found his forever home. A true success story!",
            oliverMicrochip, "ADOPTED", foster5Id, rescueOrgId);
        petSeeds.add(new PetSeed(oliverId, oliverMicrochip, "CAT", "cat-4.jpg"));

        // Create adoption application and adoption record for Oliver (the ADOPTED pet)
        UUID adopterId = getAdopterId("adopter@test.com");
        UUID vetId = getVetId("vet@test.com");
        if (adopterId != null && vetId != null) {
            // First create the adoption application (FINALIZED since Oliver is already ADOPTED)
            UUID applicationId = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO adoption_applications (id, pet_id, adopter_id, status, living_situation, pet_experience, why_adopt, submitted_at, reviewed_at)
                VALUES (?, ?, ?, 'FINALIZED', 'House with yard', 'Previous cat owner', 'Looking for a friendly companion', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                applicationId, oliverId, adopterId);

            // Then create the adoption record
            UUID adoptionId = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO adoptions (id, pet_id, foster_id, adopter_id, rescue_org_id, vet_id, application_id, adopted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                adoptionId, oliverId, foster5Id, adopterId, rescueOrgId, vetId, applicationId);
            logger.info("Created adoption application and record for Oliver");
        }

        logger.info("Created 10 sample pets across 5 fosters with various statuses");

        // Add vet sign-offs for pets that need them (AVAILABLE, IN_PROGRESS, ADOPTED)
        seedVetSignOffs(vetId, charlieId, whiskersId, dukeId, daisyId, oliverId);

        // Add adoption application for Daisy (IN_PROGRESS pet)
        if (adopterId != null) {
            UUID daisyApplicationId = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO adoption_applications (id, pet_id, adopter_id, status, living_situation, pet_experience, why_adopt, submitted_at, reviewed_at)
                VALUES (?, ?, ?, 'APPROVED', 'House with yard', 'Previous dog owner', 'Looking for a small companion dog', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                daisyApplicationId, daisyId, adopterId);
            logger.info("Created adoption application for Daisy (IN_PROGRESS)");
        }

        // Upload images for each pet
        seedPetImages(petSeeds);

        // Seed status history for each pet
        seedPetStatusHistory();
    }

    private void seedPetStatusHistory() {
        // Check if status history already exists
        Integer historyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pet_status_history", Integer.class);
        if (historyCount != null && historyCount > 0) {
            logger.debug("Pet status history already exists, skipping seeding");
            return;
        }

        logger.info("Seeding pet status history...");

        // Get rescue org user ID for rescue-related status changes
        UUID rescueOrgUserId = getUserIdByEmail("rescue@test.com");
        UUID vetUserId = getUserIdByEmail("vet@test.com");

        // Query all pets with their current status and foster user ID
        List<Map<String, Object>> pets = jdbcTemplate.queryForList("""
            SELECT p.id, p.status, p.foster_id, f.user_id as foster_user_id
            FROM pets p
            JOIN fosters f ON p.foster_id = f.id
            """);

        for (Map<String, Object> pet : pets) {
            UUID petId = (UUID) pet.get("id");
            String status = (String) pet.get("status");
            UUID fosterUserId = (UUID) pet.get("foster_user_id");

            // Always record initial DRAFT status
            insertStatusHistory(petId, null, "DRAFT", fosterUserId, "Pet registered by foster");

            // Add additional history based on current status
            switch (status) {
                case "DRAFT":
                    // Only initial DRAFT entry needed
                    break;
                case "PENDING_RESCUE":
                    insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                    break;
                case "PENDING_VET":
                    insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                    insertStatusHistory(petId, "PENDING_RESCUE", "PENDING_VET", rescueOrgUserId, "Accepted by rescue organization");
                    break;
                case "AVAILABLE":
                    insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                    insertStatusHistory(petId, "PENDING_RESCUE", "PENDING_VET", rescueOrgUserId, "Accepted by rescue organization");
                    insertStatusHistory(petId, "PENDING_VET", "AVAILABLE", vetUserId, "Signed off by veterinarian");
                    break;
                case "IN_PROGRESS":
                    insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                    insertStatusHistory(petId, "PENDING_RESCUE", "PENDING_VET", rescueOrgUserId, "Accepted by rescue organization");
                    insertStatusHistory(petId, "PENDING_VET", "AVAILABLE", vetUserId, "Signed off by veterinarian");
                    insertStatusHistory(petId, "AVAILABLE", "IN_PROGRESS", rescueOrgUserId, "Adoption application approved");
                    break;
                case "ADOPTED":
                    insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                    insertStatusHistory(petId, "PENDING_RESCUE", "PENDING_VET", rescueOrgUserId, "Accepted by rescue organization");
                    insertStatusHistory(petId, "PENDING_VET", "AVAILABLE", vetUserId, "Signed off by veterinarian");
                    insertStatusHistory(petId, "AVAILABLE", "IN_PROGRESS", rescueOrgUserId, "Adoption application approved");
                    insertStatusHistory(petId, "IN_PROGRESS", "ADOPTED", rescueOrgUserId, "Adoption finalized");
                    break;
            }
        }

        logger.info("Pet status history seeding complete for {} pets", pets.size());
    }

    private void insertStatusHistory(UUID petId, String fromStatus, String toStatus, UUID changedBy, String notes) {
        jdbcTemplate.update("""
            INSERT INTO pet_status_history (id, pet_id, from_status, to_status, changed_by, changed_at, notes)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP - INTERVAL '1 day' * (RANDOM() * 7 + 1), ?)
            """,
            UUID.randomUUID(), petId, fromStatus, toStatus, changedBy, notes);
    }

    private UUID getUserIdByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id FROM app_users WHERE email = ?", UUID.class, email);
        } catch (Exception e) {
            logger.warn("User not found for email: {}", email);
            return null;
        }
    }

    private void seedVetSignOffs(UUID vetId, UUID charlieId, UUID whiskersId, UUID dukeId, UUID daisyId, UUID oliverId) {
        if (vetId == null) {
            logger.warn("Cannot seed vet sign-offs: vet not found");
            return;
        }

        // Check if sign-offs already exist
        Integer signOffCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vet_signoffs", Integer.class);
        if (signOffCount != null && signOffCount > 0) {
            logger.debug("Vet sign-offs already exist, skipping seeding");
            return;
        }

        logger.info("Seeding vet sign-offs for AVAILABLE, IN_PROGRESS, and ADOPTED pets...");

        // Add sign-offs for all pets that should have them
        List<UUID> petsNeedingSignOff = List.of(charlieId, whiskersId, dukeId, daisyId, oliverId);

        for (UUID petId : petsNeedingSignOff) {
            UUID signOffId = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO vet_signoffs (id, pet_id, vet_id, neutered_date, health_status, health_notes, signed_off_at)
                VALUES (?, ?, ?, CURRENT_DATE - INTERVAL '30 days', 'GOOD', NULL, CURRENT_TIMESTAMP)
                """,
                signOffId, petId, vetId);
        }

        logger.info("Created vet sign-offs for {} pets", petsNeedingSignOff.size());
    }

    private void seedPetImagesIfNeeded() {
        // Check if pet_images already has entries
        Integer imageCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pet_images", Integer.class);
        if (imageCount != null && imageCount > 0) {
            logger.debug("Pet images already exist, skipping image seeding");
            return;
        }

        // Get all pets and seed images for them
        List<Map<String, Object>> pets = jdbcTemplate.queryForList(
            "SELECT id, microchip_id, species FROM pets"
        );

        if (pets.isEmpty()) {
            return;
        }

        // Map species to appropriate demo images
        int dogIndex = 1;
        int catIndex = 1;
        List<PetSeed> petSeeds = new ArrayList<>();

        for (Map<String, Object> pet : pets) {
            UUID petId = (UUID) pet.get("id");
            String microchipId = (String) pet.get("microchip_id");
            String species = (String) pet.get("species");

            String imageFile;
            if ("DOG".equals(species)) {
                imageFile = "dog-" + dogIndex + ".jpg";
                dogIndex = (dogIndex % 6) + 1; // Cycle through 1-6
            } else {
                imageFile = "cat-" + catIndex + ".jpg";
                catIndex = (catIndex % 4) + 1; // Cycle through 1-4
            }
            petSeeds.add(new PetSeed(petId, microchipId, species, imageFile));
        }

        seedPetImages(petSeeds);
    }

    private void seedPetImages(List<PetSeed> petSeeds) {
        logger.info("Uploading demo images to S3 for {} pets...", petSeeds.size());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (PetSeed seed : petSeeds) {
            try {
                Resource imageResource = resolver.getResource("classpath:demo-images/" + seed.imageFile());

                if (!imageResource.exists()) {
                    logger.warn("Demo image not found: {}", seed.imageFile());
                    continue;
                }

                // Create a MultipartFile from the resource
                byte[] imageBytes = imageResource.getInputStream().readAllBytes();
                MultipartFile multipartFile = new ByteArrayMultipartFile(
                    seed.imageFile(),
                    seed.imageFile(),
                    "image/jpeg",
                    imageBytes
                );

                // Upload to S3 using microchip ID for folder and sequential number for filename
                // Format: pets/<microchip_id>/1.jpg
                String s3Key = "pets/" + seed.microchipId() + "/1.jpg";
                storageService.uploadFileWithKey(multipartFile, s3Key);

                // Create pet_images record
                jdbcTemplate.update("""
                    INSERT INTO pet_images (id, pet_id, s3_key, is_primary, display_order, uploaded_at)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    UUID.randomUUID(), seed.petId(), s3Key, true, 0);

                logger.debug("Uploaded image for pet {} (microchip {}): {}", seed.petId(), seed.microchipId(), s3Key);

            } catch (IOException e) {
                logger.error("Failed to upload image for pet {}: {}", seed.petId(), e.getMessage());
            }
        }

        logger.info("Completed uploading demo images to S3");
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

    private UUID getAdopterId(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT a.id FROM adopters a JOIN app_users u ON a.user_id = u.id WHERE u.email = ?",
                UUID.class, email);
        } catch (Exception e) {
            logger.warn("Adopter not found for email: {}", email);
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

    private void seedVetApprovalRequests() {
        // Check if vet_approval_requests table exists (migration V23)
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'vet_approval_requests'",
                Integer.class);
            if (tableCount == null || tableCount == 0) {
                logger.debug("vet_approval_requests table does not exist yet, skipping approval request seeding");
                return;
            }
        } catch (Exception e) {
            logger.debug("Failed to check vet_approval_requests table: {}", e.getMessage());
            return;
        }

        // Check if requests already exist
        Integer requestCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vet_approval_requests", Integer.class);
        if (requestCount != null && requestCount > 0) {
            logger.debug("Vet approval requests already exist, skipping seeding");
            return;
        }

        // Get the vet and rescue org IDs
        UUID vetId = getVetId("vet@test.com");
        UUID rescueOrgId = getRescueOrgId("rescue@test.com");

        if (vetId == null || rescueOrgId == null) {
            logger.warn("Cannot seed vet approval requests: vet or rescue org not found");
            return;
        }

        // The test vet is already approved by the test rescue org via vet_approvals
        // But we can create a sample PENDING request to show the approval request workflow
        // We'll skip this since the test vet is already approved

        logger.info("Test vet is already approved by test rescue org - no pending requests to seed");
    }

    private UUID getVetId(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT v.id FROM vets v JOIN app_users u ON v.user_id = u.id WHERE u.email = ?",
                UUID.class, email);
        } catch (Exception e) {
            logger.warn("Vet not found for email: {}", email);
            return null;
        }
    }

    private UUID getRescueOrgId(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT r.id FROM rescue_organizations r JOIN app_users u ON r.user_id = u.id WHERE u.email = ?",
                UUID.class, email);
        } catch (Exception e) {
            logger.warn("Rescue org not found for email: {}", email);
            return null;
        }
    }

    private record TestAccount(String email, String name, UserRole role, String firstName, String lastName) {}
    private record PetSeed(UUID petId, String microchipId, String species, String imageFile) {}

    /**
     * Simple MultipartFile implementation for seeding demo images.
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
