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
import java.util.Random;

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
            new TestAccount("superadmin@test.com", "Super Admin", UserRole.SUPER_ADMIN, null, null),
            new TestAccount("admin@test.com", "Test Admin", UserRole.ADMIN, null, null),
            // 4 Fosters (1 per rescue)
            new TestAccount("foster-r1@test.com", "Alex", UserRole.FOSTER, "Alex", "Johnson"),
            new TestAccount("foster-r2@test.com", "Jordan", UserRole.FOSTER, "Jordan", "Smith"),
            new TestAccount("foster-r3@test.com", "Taylor", UserRole.FOSTER, "Taylor", "Brown"),
            new TestAccount("foster-r4@test.com", "Morgan", UserRole.FOSTER, "Morgan", "Davis"),
            new TestAccount("adopter@test.com", "Test Adopter", UserRole.ADOPTER, null, null),
            new TestAccount("vet@test.com", "Test Vet", UserRole.VET, null, null),
            // 4 Rescue Organizations
            new TestAccount("rescue1@test.com", "Happy Tails Rescue", UserRole.RESCUE_ORG, null, null),
            new TestAccount("rescue2@test.com", "Second Chance Animal Shelter", UserRole.RESCUE_ORG, null, null),
            new TestAccount("rescue3@test.com", "Paws & Claws Rescue", UserRole.RESCUE_ORG, null, null),
            new TestAccount("rescue4@test.com", "Forever Friends Animal Rescue", UserRole.RESCUE_ORG, null, null)
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

    // Pet names for variety
    private static final String[] DOG_NAMES = {"Max", "Buddy", "Charlie", "Rocky", "Duke", "Cooper", "Bear", "Tucker", "Jack", "Zeus",
        "Toby", "Bailey", "Milo", "Winston", "Murphy", "Oscar", "Finn", "Archie", "Bruno", "Leo"};
    private static final String[] CAT_NAMES = {"Luna", "Milo", "Oliver", "Leo", "Bella", "Lily", "Chloe", "Lucy", "Sophie", "Nala",
        "Willow", "Simba", "Whiskers", "Shadow", "Tiger", "Jasper", "Felix", "Ginger", "Midnight", "Pumpkin"};
    private static final String[] DOG_BREEDS = {"LABRADOR", "GOLDEN_RETRIEVER", "GERMAN_SHEPHERD", "FRENCH_BULLDOG",
        "BULLDOG", "BEAGLE", "POODLE", "POODLE_MIX", "ROTTWEILER", "YORKSHIRE_TERRIER", "BOXER", "DACHSHUND",
        "HUSKY", "SIBERIAN_HUSKY", "GREAT_DANE", "DOBERMAN", "AUSTRALIAN_SHEPHERD", "CAVALIER_KING_CHARLES",
        "SHIH_TZU", "BOSTON_TERRIER", "BERNESE_MOUNTAIN_DOG", "POMERANIAN", "HAVANESE", "SHETLAND_SHEEPDOG",
        "CORGI", "PEMBROKE_WELSH_CORGI", "COCKER_SPANIEL", "BORDER_COLLIE", "CHIHUAHUA", "MINIATURE_SCHNAUZER",
        "PIT_BULL", "MALTESE", "JACK_RUSSELL_TERRIER", "BICHON_FRISE", "MIXED_DOG"};
    private static final String[] CAT_BREEDS = {"TABBY", "DOMESTIC_SHORTHAIR", "DOMESTIC_LONGHAIR", "SIAMESE",
        "MAINE_COON", "PERSIAN", "RAGDOLL", "BENGAL", "BRITISH_SHORTHAIR", "ABYSSINIAN", "BIRMAN",
        "ORIENTAL_SHORTHAIR", "SPHYNX", "DEVON_REX", "SCOTTISH_FOLD", "RUSSIAN_BLUE", "AMERICAN_SHORTHAIR",
        "NORWEGIAN_FOREST_CAT", "EXOTIC_SHORTHAIR", "BURMESE", "TONKINESE", "HIMALAYAN", "TURKISH_ANGORA",
        "RAGAMUFFIN", "MIXED_CAT"};
    private static final String[] SIZES = {"SMALL", "MEDIUM", "LARGE"};
    private static final String[] SEXES = {"MALE", "FEMALE"};

    private void seedPets() {
        // Check if pets already exist
        Integer petCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pets", Integer.class);
        if (petCount != null && petCount > 0) {
            logger.debug("Pets already exist, skipping pet seeding");
            // Still check if images need to be seeded
            seedPetImagesIfNeeded();
            return;
        }

        // Get all 4 rescue org IDs
        UUID rescue1Id = getRescueOrgId("rescue1@test.com");
        UUID rescue2Id = getRescueOrgId("rescue2@test.com");
        UUID rescue3Id = getRescueOrgId("rescue3@test.com");
        UUID rescue4Id = getRescueOrgId("rescue4@test.com");

        if (rescue1Id == null || rescue2Id == null || rescue3Id == null || rescue4Id == null) {
            logger.warn("Cannot seed pets: one or more rescue orgs not found");
            return;
        }

        // Get all 4 foster IDs (1 per rescue)
        UUID foster1Id = getFosterId("foster-r1@test.com");
        UUID foster2Id = getFosterId("foster-r2@test.com");
        UUID foster3Id = getFosterId("foster-r3@test.com");
        UUID foster4Id = getFosterId("foster-r4@test.com");

        logger.info("Seeding 40 sample pets across 4 rescue organizations...");

        // Store pet IDs for image association and vet sign-offs
        List<PetSeed> petSeeds = new ArrayList<>();
        List<UUID> petsNeedingVetSignOff = new ArrayList<>();
        List<UUID> inProgressPetIds = new ArrayList<>();
        List<UUID> adoptedPetIds = new ArrayList<>();
        List<UUID> adoptedPetFosterIds = new ArrayList<>();
        List<UUID> adoptedPetRescueIds = new ArrayList<>();

        // Seed pets for each rescue (10 pets each: 5 dogs, 5 cats; 1 foster-owned, 9 rescue-owned)
        seedPetsForRescue(1, rescue1Id, foster1Id, petSeeds, petsNeedingVetSignOff, inProgressPetIds, adoptedPetIds, adoptedPetFosterIds, adoptedPetRescueIds);
        seedPetsForRescue(2, rescue2Id, foster2Id, petSeeds, petsNeedingVetSignOff, inProgressPetIds, adoptedPetIds, adoptedPetFosterIds, adoptedPetRescueIds);
        seedPetsForRescue(3, rescue3Id, foster3Id, petSeeds, petsNeedingVetSignOff, inProgressPetIds, adoptedPetIds, adoptedPetFosterIds, adoptedPetRescueIds);
        seedPetsForRescue(4, rescue4Id, foster4Id, petSeeds, petsNeedingVetSignOff, inProgressPetIds, adoptedPetIds, adoptedPetFosterIds, adoptedPetRescueIds);

        logger.info("Created 40 sample pets across 4 rescues (10 per rescue: 5 dogs, 5 cats)");

        // Get vet and adopter IDs for sign-offs and applications
        UUID vetId = getVetId("vet@test.com");
        UUID adopterId = getAdopterId("adopter@test.com");

        // Add vet sign-offs for pets that need them (AVAILABLE, IN_PROGRESS, ADOPTED)
        seedVetSignOffsForPets(vetId, petsNeedingVetSignOff);

        // Add adoption applications for IN_PROGRESS pets
        seedAdoptionApplications(adopterId, inProgressPetIds, "APPROVED");

        // Add adoption applications and records for ADOPTED pets
        for (int i = 0; i < adoptedPetIds.size(); i++) {
            UUID petId = adoptedPetIds.get(i);
            UUID fosterId = adoptedPetFosterIds.get(i);
            UUID rescueOrgId = adoptedPetRescueIds.get(i);

            if (adopterId != null && vetId != null) {
                UUID applicationId = UUID.randomUUID();
                jdbcTemplate.update("""
                    INSERT INTO adoption_applications (id, pet_id, adopter_id, status, living_situation, pet_experience, why_adopt, submitted_at, reviewed_at)
                    VALUES (?, ?, ?, 'FINALIZED', 'House with yard', 'Previous pet owner', 'Looking for a loving companion', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    applicationId, petId, adopterId);

                UUID adoptionId = UUID.randomUUID();
                jdbcTemplate.update("""
                    INSERT INTO adoptions (id, pet_id, foster_id, adopter_id, rescue_org_id, vet_id, application_id, adopted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    adoptionId, petId, fosterId, adopterId, rescueOrgId, vetId, applicationId);
            }
        }
        logger.info("Created adoption records for {} adopted pets", adoptedPetIds.size());

        // Upload images for each pet
        seedPetImages(petSeeds);

        // Seed status history for each pet
        seedPetStatusHistory();
    }

    /**
     * Seeds 10 pets for a single rescue organization.
     * Distribution: 5 dogs + 5 cats, 1 foster-owned + 9 rescue-owned
     * Statuses: 1 AVAILABLE (foster), 2 PENDING_VET, 5 AVAILABLE, 1 IN_PROGRESS, 1 ADOPTED
     */
    private void seedPetsForRescue(int rescueNumber, UUID rescueOrgId, UUID fosterId,
                                    List<PetSeed> petSeeds, List<UUID> petsNeedingVetSignOff,
                                    List<UUID> inProgressPetIds, List<UUID> adoptedPetIds,
                                    List<UUID> adoptedPetFosterIds, List<UUID> adoptedPetRescueIds) {

        int nameOffset = (rescueNumber - 1) * 5; // Each rescue uses different names
        Random random = new Random(rescueNumber); // Deterministic randomness per rescue

        // Pet 1: Dog, Foster-owned, ADOPTED (foster-owned so we can create adoption record with foster_id)
        UUID pet1Id = createPet(rescueNumber, 1, "DOG", DOG_NAMES[nameOffset], DOG_BREEDS[nameOffset % DOG_BREEDS.length],
            "ADOPTED", fosterId, rescueOrgId, petSeeds, random);
        petsNeedingVetSignOff.add(pet1Id);
        adoptedPetIds.add(pet1Id);
        adoptedPetFosterIds.add(fosterId); // Foster-owned, has foster
        adoptedPetRescueIds.add(rescueOrgId);

        // Pet 2: Dog, Rescue-owned, PENDING_VET
        createPet(rescueNumber, 2, "DOG", DOG_NAMES[nameOffset + 1], DOG_BREEDS[(nameOffset + 1) % DOG_BREEDS.length],
            "PENDING_VET", null, rescueOrgId, petSeeds, random);

        // Pet 3: Dog, Rescue-owned, AVAILABLE
        UUID pet3Id = createPet(rescueNumber, 3, "DOG", DOG_NAMES[nameOffset + 2], DOG_BREEDS[(nameOffset + 2) % DOG_BREEDS.length],
            "AVAILABLE", null, rescueOrgId, petSeeds, random);
        petsNeedingVetSignOff.add(pet3Id);

        // Pet 4: Dog, Rescue-owned, AVAILABLE
        UUID pet4Id = createPet(rescueNumber, 4, "DOG", DOG_NAMES[nameOffset + 3], DOG_BREEDS[(nameOffset + 3) % DOG_BREEDS.length],
            "AVAILABLE", null, rescueOrgId, petSeeds, random);
        petsNeedingVetSignOff.add(pet4Id);

        // Pet 5: Dog, Rescue-owned, IN_PROGRESS
        UUID pet5Id = createPet(rescueNumber, 5, "DOG", DOG_NAMES[nameOffset + 4], DOG_BREEDS[(nameOffset + 4) % DOG_BREEDS.length],
            "IN_PROGRESS", null, rescueOrgId, petSeeds, random);
        petsNeedingVetSignOff.add(pet5Id);
        inProgressPetIds.add(pet5Id);

        // Pet 6: Cat, Rescue-owned, PENDING_VET
        createPet(rescueNumber, 6, "CAT", CAT_NAMES[nameOffset], CAT_BREEDS[nameOffset % CAT_BREEDS.length],
            "PENDING_VET", null, rescueOrgId, petSeeds, random);

        // Pet 7: Cat, Rescue-owned, AVAILABLE
        UUID pet7Id = createPet(rescueNumber, 7, "CAT", CAT_NAMES[nameOffset + 1], CAT_BREEDS[(nameOffset + 1) % CAT_BREEDS.length],
            "AVAILABLE", null, rescueOrgId, petSeeds, random);
        petsNeedingVetSignOff.add(pet7Id);

        // Pet 8: Cat, Rescue-owned, AVAILABLE
        UUID pet8Id = createPet(rescueNumber, 8, "CAT", CAT_NAMES[nameOffset + 2], CAT_BREEDS[(nameOffset + 2) % CAT_BREEDS.length],
            "AVAILABLE", null, rescueOrgId, petSeeds, random);
        petsNeedingVetSignOff.add(pet8Id);

        // Pet 9: Cat, Rescue-owned, AVAILABLE
        UUID pet9Id = createPet(rescueNumber, 9, "CAT", CAT_NAMES[nameOffset + 3], CAT_BREEDS[(nameOffset + 3) % CAT_BREEDS.length],
            "AVAILABLE", null, rescueOrgId, petSeeds, random);
        petsNeedingVetSignOff.add(pet9Id);

        // Pet 10: Cat, Rescue-owned, AVAILABLE (changed from ADOPTED since adoptions table requires foster_id)
        UUID pet10Id = createPet(rescueNumber, 10, "CAT", CAT_NAMES[nameOffset + 4], CAT_BREEDS[(nameOffset + 4) % CAT_BREEDS.length],
            "AVAILABLE", null, rescueOrgId, petSeeds, random);
        petsNeedingVetSignOff.add(pet10Id);

        logger.debug("Seeded 10 pets for rescue {} (5 dogs, 5 cats)", rescueNumber);
    }

    /**
     * Creates a single pet and adds it to the pet seeds list.
     */
    private UUID createPet(int rescueNumber, int petNumber, String species, String name, String breed,
                           String status, UUID fosterId, UUID rescueOrgId, List<PetSeed> petSeeds, Random random) {
        UUID petId = UUID.randomUUID();
        String microchip = String.format("CHIP-R%d-%s-%02d", rescueNumber, species.substring(0, 1), petNumber);

        int age = random.nextInt(10) + 1;
        String ageUnit = age <= 11 ? "MONTHS" : "YEARS";
        if (ageUnit.equals("YEARS")) age = random.nextInt(12) + 1;

        String size = SIZES[random.nextInt(SIZES.length)];
        String sex = SEXES[random.nextInt(SEXES.length)];

        String description = generateDescription(name, species, breed, age, ageUnit, sex);

        String imageFile = getBreedImageFile(breed);

        jdbcTemplate.update("""
            INSERT INTO pets (id, name, species, breed, age, age_unit, sex, size, description, microchip_id,
                status, foster_id, rescue_org_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            petId, name, species, breed, age, ageUnit, sex, size, description, microchip, status, fosterId, rescueOrgId);

        petSeeds.add(new PetSeed(petId, microchip, species, imageFile));
        return petId;
    }

    /**
     * Returns the image filename for a given breed enum.
     * Each breed has a unique breed-specific image.
     */
    private String getBreedImageFile(String breed) {
        return switch (breed) {
            // Dog breeds
            case "LABRADOR" -> "labrador.jpg";
            case "GOLDEN_RETRIEVER" -> "golden-retriever.jpg";
            case "GERMAN_SHEPHERD" -> "german-shepherd.jpg";
            case "FRENCH_BULLDOG" -> "french-bulldog.jpg";
            case "BULLDOG" -> "bulldog.jpg";
            case "BEAGLE" -> "beagle.jpg";
            case "POODLE" -> "poodle.jpg";
            case "POODLE_MIX" -> "poodle-mix.jpg";
            case "ROTTWEILER" -> "rottweiler.jpg";
            case "YORKSHIRE_TERRIER" -> "yorkshire-terrier.jpg";
            case "BOXER" -> "boxer.jpg";
            case "DACHSHUND" -> "dachshund.jpg";
            case "HUSKY" -> "husky.jpg";
            case "SIBERIAN_HUSKY" -> "siberian-husky.jpg";
            case "GREAT_DANE" -> "great-dane.jpg";
            case "DOBERMAN" -> "doberman.jpg";
            case "AUSTRALIAN_SHEPHERD" -> "australian-shepherd.jpg";
            case "CAVALIER_KING_CHARLES" -> "cavalier-king-charles.jpg";
            case "SHIH_TZU" -> "shih-tzu.jpg";
            case "BOSTON_TERRIER" -> "boston-terrier.jpg";
            // Cat breeds
            case "TABBY" -> "tabby.jpg";
            case "DOMESTIC_SHORTHAIR" -> "domestic-shorthair.jpg";
            case "DOMESTIC_LONGHAIR" -> "domestic-longhair.jpg";
            case "SIAMESE" -> "siamese.jpg";
            case "MAINE_COON" -> "maine-coon.jpg";
            case "PERSIAN" -> "persian.jpg";
            case "RAGDOLL" -> "ragdoll.jpg";
            case "BENGAL" -> "bengal.jpg";
            case "BRITISH_SHORTHAIR" -> "british-shorthair.jpg";
            case "ABYSSINIAN" -> "abyssinian.jpg";
            case "BIRMAN" -> "birman.jpg";
            case "ORIENTAL_SHORTHAIR" -> "oriental-shorthair.jpg";
            case "SPHYNX" -> "sphynx.jpg";
            case "DEVON_REX" -> "devon-rex.jpg";
            case "SCOTTISH_FOLD" -> "scottish-fold.jpg";
            case "RUSSIAN_BLUE" -> "russian-blue.jpg";
            case "AMERICAN_SHORTHAIR" -> "american-shorthair.jpg";
            case "NORWEGIAN_FOREST_CAT" -> "norwegian-forest-cat.jpg";
            case "EXOTIC_SHORTHAIR" -> "exotic-shorthair.jpg";
            case "BURMESE" -> "burmese.jpg";
            // Fallback for any other breeds
            default -> breed.toLowerCase().contains("dog") || breed.toLowerCase().contains("mix_dog")
                ? "dog-1.jpg"
                : "cat-1.jpg";
        };
    }

    /**
     * Generates a description for a pet based on its attributes.
     */
    private String generateDescription(String name, String species, String breedEnum, int age, String ageUnit, String sex) {
        String pronoun = sex.equals("MALE") ? "He" : "She";
        String possessive = sex.equals("MALE") ? "his" : "her";
        String speciesWord = species.equals("DOG") ? "dog" : "cat";
        String breed = formatBreedName(breedEnum);

        String[] descriptions = {
            name + " is a wonderful " + breed + " looking for " + possessive + " forever home. " + pronoun + " loves cuddles and playtime!",
            "Meet " + name + ", a charming " + age + " " + ageUnit.toLowerCase() + " old " + breed + ". " + pronoun + "'s friendly, well-socialized, and ready for adoption.",
            name + " is an adorable " + breed + " with a heart of gold. " + pronoun + " gets along great with everyone " + pronoun.toLowerCase() + " meets.",
            "This lovely " + breed + " named " + name + " is looking for a loving family. " + pronoun + "'s " + age + " " + ageUnit.toLowerCase() + " old and full of energy!",
            name + " is a sweet " + speciesWord + " who would make a perfect companion. " + pronoun + " enjoys " + (species.equals("DOG") ? "walks and fetch" : "sunny spots and toys") + "."
        };

        return descriptions[Math.abs(name.hashCode()) % descriptions.length];
    }

    /**
     * Converts a breed enum key to a human-readable display name.
     * E.g., "GOLDEN_RETRIEVER" -> "Golden Retriever"
     */
    private String formatBreedName(String breedEnum) {
        if (breedEnum == null || breedEnum.isBlank()) {
            return "mixed breed";
        }
        String[] words = breedEnum.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    /**
     * Seeds vet sign-offs for a list of pet IDs.
     */
    private void seedVetSignOffsForPets(UUID vetId, List<UUID> petIds) {
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

        logger.info("Seeding vet sign-offs for {} pets...", petIds.size());

        for (UUID petId : petIds) {
            UUID signOffId = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO vet_signoffs (id, pet_id, vet_id, neutered_date, health_status, health_notes, signed_off_at)
                VALUES (?, ?, ?, CURRENT_DATE - INTERVAL '30 days', 'GOOD', NULL, CURRENT_TIMESTAMP)
                """,
                signOffId, petId, vetId);
        }

        logger.info("Created vet sign-offs for {} pets", petIds.size());
    }

    /**
     * Seeds adoption applications for a list of pet IDs.
     */
    private void seedAdoptionApplications(UUID adopterId, List<UUID> petIds, String status) {
        if (adopterId == null) {
            logger.warn("Cannot seed adoption applications: adopter not found");
            return;
        }

        logger.info("Seeding adoption applications for {} pets...", petIds.size());

        for (UUID petId : petIds) {
            UUID applicationId = UUID.randomUUID();
            jdbcTemplate.update("""
                INSERT INTO adoption_applications (id, pet_id, adopter_id, status, living_situation, pet_experience, why_adopt, submitted_at, reviewed_at)
                VALUES (?, ?, ?, ?, 'House with yard', 'Previous pet owner', 'Looking for a loving companion', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                applicationId, petId, adopterId, status);
        }

        logger.info("Created {} adoption applications", petIds.size());
    }

    private void seedPetStatusHistory() {
        // Check if status history already exists
        Integer historyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pet_status_history", Integer.class);
        if (historyCount != null && historyCount > 0) {
            logger.debug("Pet status history already exists, skipping seeding");
            return;
        }

        logger.info("Seeding pet status history...");

        UUID vetUserId = getUserIdByEmail("vet@test.com");

        // Query all pets with their current status, foster user ID (if any), and rescue org user ID
        List<Map<String, Object>> pets = jdbcTemplate.queryForList("""
            SELECT p.id, p.status, p.foster_id, p.rescue_org_id,
                   f.user_id as foster_user_id,
                   r.user_id as rescue_user_id
            FROM pets p
            LEFT JOIN fosters f ON p.foster_id = f.id
            LEFT JOIN rescue_organizations r ON p.rescue_org_id = r.id
            """);

        for (Map<String, Object> pet : pets) {
            UUID petId = (UUID) pet.get("id");
            String status = (String) pet.get("status");
            UUID fosterId = (UUID) pet.get("foster_id");
            UUID fosterUserId = (UUID) pet.get("foster_user_id");
            UUID rescueUserId = (UUID) pet.get("rescue_user_id");

            boolean isRescueOwned = (fosterId == null);

            if (isRescueOwned) {
                // Rescue-owned pets start at PENDING_VET (skip DRAFT and PENDING_RESCUE)
                insertStatusHistory(petId, null, "PENDING_VET", rescueUserId, "Pet registered by rescue organization");

                switch (status) {
                    case "PENDING_VET":
                        // Only initial PENDING_VET entry needed
                        break;
                    case "AVAILABLE":
                        insertStatusHistory(petId, "PENDING_VET", "AVAILABLE", vetUserId, "Signed off by veterinarian");
                        break;
                    case "IN_PROGRESS":
                        insertStatusHistory(petId, "PENDING_VET", "AVAILABLE", vetUserId, "Signed off by veterinarian");
                        insertStatusHistory(petId, "AVAILABLE", "IN_PROGRESS", rescueUserId, "Adoption application approved");
                        break;
                    case "ADOPTED":
                        insertStatusHistory(petId, "PENDING_VET", "AVAILABLE", vetUserId, "Signed off by veterinarian");
                        insertStatusHistory(petId, "AVAILABLE", "IN_PROGRESS", rescueUserId, "Adoption application approved");
                        insertStatusHistory(petId, "IN_PROGRESS", "ADOPTED", rescueUserId, "Adoption finalized");
                        break;
                }
            } else {
                // Foster-owned pets go through full workflow
                insertStatusHistory(petId, null, "DRAFT", fosterUserId, "Pet registered by foster");

                switch (status) {
                    case "DRAFT":
                        // Only initial DRAFT entry needed
                        break;
                    case "PENDING_RESCUE":
                        insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                        break;
                    case "PENDING_VET":
                        insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                        insertStatusHistory(petId, "PENDING_RESCUE", "PENDING_VET", rescueUserId, "Accepted by rescue organization");
                        break;
                    case "AVAILABLE":
                        insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                        insertStatusHistory(petId, "PENDING_RESCUE", "PENDING_VET", rescueUserId, "Accepted by rescue organization");
                        insertStatusHistory(petId, "PENDING_VET", "AVAILABLE", vetUserId, "Signed off by veterinarian");
                        break;
                    case "IN_PROGRESS":
                        insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                        insertStatusHistory(petId, "PENDING_RESCUE", "PENDING_VET", rescueUserId, "Accepted by rescue organization");
                        insertStatusHistory(petId, "PENDING_VET", "AVAILABLE", vetUserId, "Signed off by veterinarian");
                        insertStatusHistory(petId, "AVAILABLE", "IN_PROGRESS", rescueUserId, "Adoption application approved");
                        break;
                    case "ADOPTED":
                        insertStatusHistory(petId, "DRAFT", "PENDING_RESCUE", fosterUserId, "Submitted for rescue review");
                        insertStatusHistory(petId, "PENDING_RESCUE", "PENDING_VET", rescueUserId, "Accepted by rescue organization");
                        insertStatusHistory(petId, "PENDING_VET", "AVAILABLE", vetUserId, "Signed off by veterinarian");
                        insertStatusHistory(petId, "AVAILABLE", "IN_PROGRESS", rescueUserId, "Adoption application approved");
                        insertStatusHistory(petId, "IN_PROGRESS", "ADOPTED", rescueUserId, "Adoption finalized");
                        break;
                }
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

    private void seedPetImagesIfNeeded() {
        // Check if pet_images already has entries
        Integer imageCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pet_images", Integer.class);
        if (imageCount != null && imageCount > 0) {
            logger.debug("Pet images already exist, skipping image seeding");
            return;
        }

        // Get all pets and seed images for them using breed-specific images
        List<Map<String, Object>> pets = jdbcTemplate.queryForList(
            "SELECT id, microchip_id, species, breed FROM pets"
        );

        if (pets.isEmpty()) {
            return;
        }

        List<PetSeed> petSeeds = new ArrayList<>();

        for (Map<String, Object> pet : pets) {
            UUID petId = (UUID) pet.get("id");
            String microchipId = (String) pet.get("microchip_id");
            String species = (String) pet.get("species");
            String breed = (String) pet.get("breed");

            String imageFile = getBreedImageFile(breed);
            petSeeds.add(new PetSeed(petId, microchipId, species, imageFile));
        }

        seedPetImages(petSeeds);
    }

    // Generic images for additional pet photos
    private static final String[] EXTRA_DOG_IMAGES = {"dog-1.jpg", "dog-2.jpg", "dog-3.jpg", "dog-4.jpg", "dog-5.jpg", "dog-6.jpg"};
    private static final String[] EXTRA_CAT_IMAGES = {"cat-1.jpg", "cat-2.jpg", "cat-3.jpg", "cat-4.jpg"};

    private void seedPetImages(List<PetSeed> petSeeds) {
        logger.info("Uploading demo images to S3 for {} pets...", petSeeds.size());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Random random = new Random(42); // Deterministic for reproducibility

        int petsWithMultipleImages = 0;

        for (int i = 0; i < petSeeds.size(); i++) {
            PetSeed seed = petSeeds.get(i);
            try {
                // Upload the primary breed-specific image
                Resource imageResource = resolver.getResource("classpath:demo-images/" + seed.imageFile());

                if (!imageResource.exists()) {
                    logger.warn("Demo image not found: {}", seed.imageFile());
                    continue;
                }

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

                // Create pet_images record for primary image
                jdbcTemplate.update("""
                    INSERT INTO pet_images (id, pet_id, s3_key, is_primary, display_order, uploaded_at)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    UUID.randomUUID(), seed.petId(), s3Key, true, 0);

                logger.debug("Uploaded primary image for pet {} (microchip {}): {}", seed.petId(), seed.microchipId(), s3Key);

                // Add multiple images to some pets (roughly every 3rd pet gets 2-3 images)
                if (i % 3 == 0) {
                    int extraImageCount = random.nextInt(2) + 1; // 1 or 2 extra images
                    String[] extraImages = seed.species().equals("DOG") ? EXTRA_DOG_IMAGES : EXTRA_CAT_IMAGES;

                    for (int j = 0; j < extraImageCount && j < extraImages.length; j++) {
                        String extraImageFile = extraImages[(i + j) % extraImages.length];
                        Resource extraResource = resolver.getResource("classpath:demo-images/" + extraImageFile);

                        if (!extraResource.exists()) {
                            continue;
                        }

                        byte[] extraBytes = extraResource.getInputStream().readAllBytes();
                        MultipartFile extraMultipart = new ByteArrayMultipartFile(
                            extraImageFile,
                            extraImageFile,
                            "image/jpeg",
                            extraBytes
                        );

                        String extraS3Key = "pets/" + seed.microchipId() + "/" + (j + 2) + ".jpg";
                        storageService.uploadFileWithKey(extraMultipart, extraS3Key);

                        jdbcTemplate.update("""
                            INSERT INTO pet_images (id, pet_id, s3_key, is_primary, display_order, uploaded_at)
                            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                            """,
                            UUID.randomUUID(), seed.petId(), extraS3Key, false, j + 1);

                        logger.debug("Uploaded extra image {} for pet {}: {}", j + 2, seed.petId(), extraS3Key);
                    }
                    petsWithMultipleImages++;
                }

            } catch (IOException e) {
                logger.error("Failed to upload image for pet {}: {}", seed.petId(), e.getMessage());
            }
        }

        logger.info("Completed uploading demo images to S3 ({} pets have multiple images)", petsWithMultipleImages);
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
                String rescueName = account.name();
                String slug = rescueName.toLowerCase().replaceAll("[^a-z0-9]+", "-");
                String website = "https://" + slug + ".org";
                String description = rescueName + " is a dedicated animal rescue organization committed to finding forever homes for pets in need.";
                // Use different Irish locations for each rescue
                String[] streets = {"12 Grafton Street", "45 Shop Street", "78 Patrick Street", "23 Main Street"};
                String[] cities = {"Dublin", "Galway", "Cork", "Limerick"};
                String[] counties = {"Dublin", "Galway", "Cork", "Limerick"};
                String[] eircodes = {"D02 VK60", "H91 E2K3", "T12 W8HK", "V94 T9PX"};
                int rescueIndex = account.email().charAt(6) - '1'; // Extract 1-4 from rescue1@, rescue2@, etc.
                jdbcTemplate.update("""
                    INSERT INTO rescue_organizations (id, user_id, name, phone, website, description,
                        contact_name, contact_email, verified, address_street, address_city, address_state, address_postal_code, address_country)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id, user.getId(), rescueName, "+353 1 " + (555_0100 + rescueIndex),
                    website, description,
                    "Contact Person", account.email(), true,
                    streets[rescueIndex], cities[rescueIndex], counties[rescueIndex], eircodes[rescueIndex], "Ireland");
            }
            case ADMIN, SUPER_ADMIN -> {
                // Admin/Super Admin doesn't need a profile entity
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
                    String rescueName = account.name();
                    String slug = rescueName.toLowerCase().replaceAll("[^a-z0-9]+", "-");
                    String website = "https://" + slug + ".org";
                    String description = rescueName + " is a dedicated animal rescue organization committed to finding forever homes for pets in need.";
                    // Use different Irish locations for each rescue
                    String[] streets = {"12 Grafton Street", "45 Shop Street", "78 Patrick Street", "23 Main Street"};
                    String[] cities = {"Dublin", "Galway", "Cork", "Limerick"};
                    String[] counties = {"Dublin", "Galway", "Cork", "Limerick"};
                    String[] eircodes = {"D02 VK60", "H91 E2K3", "T12 W8HK", "V94 T9PX"};
                    int rescueIndex = account.email().charAt(6) - '1';
                    jdbcTemplate.update("""
                        INSERT INTO rescue_organizations (id, user_id, name, phone, website, description,
                            contact_name, contact_email, verified, address_street, address_city, address_state, address_postal_code, address_country)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        id, user.getId(), rescueName, "+353 1 " + (555_0100 + rescueIndex),
                        website, description,
                        "Contact Person", account.email(), true,
                        streets[rescueIndex], cities[rescueIndex], counties[rescueIndex], eircodes[rescueIndex], "Ireland");
                    logger.info("Created missing RescueOrganization profile for: {}", account.email());
                }
            }
            case ADMIN, SUPER_ADMIN -> {
                // Admin/Super Admin doesn't need a profile entity
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

        // Get the vet and first rescue org IDs
        UUID vetId = getVetId("vet@test.com");
        UUID rescueOrgId = getRescueOrgId("rescue1@test.com");

        if (vetId == null || rescueOrgId == null) {
            logger.warn("Cannot seed vet approval requests: vet or rescue org not found");
            return;
        }

        // The test vet is already approved by test rescue orgs via vet_approvals
        // But we can create a sample PENDING request to show the approval request workflow
        // We'll skip this since the test vet is already approved

        logger.info("Test vet is available for all rescue orgs - no pending requests to seed");
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
