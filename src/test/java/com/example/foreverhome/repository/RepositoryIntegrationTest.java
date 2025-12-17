package com.example.foreverhome.repository;

import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.domain.adoption.ApplicationStatus;
import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.Breed;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.pet.Species;
import com.example.foreverhome.domain.user.AccountStatus;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for repositories using Testcontainers PostgreSQL.
 * These tests verify custom queries work correctly with a real database.
 *
 * Disabled by default - enable when Docker is available.
 * To run: mvn test -Dtest=RepositoryIntegrationTest -DskipTests=false
 */
@SpringBootTest
@Testcontainers
@Disabled("Requires Docker - run manually when Docker is available")
@DisplayName("Repository Integration Tests")
class RepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("foreverhome_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private AdoptionApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID fosterId;
    private UUID rescueOrgId;
    private UUID adopterId;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        applicationRepository.deleteAll();
        petRepository.deleteAll();
        userRepository.deleteAll();

        // Create prerequisite IDs (these would normally be from profile tables)
        fosterId = UUID.randomUUID();
        rescueOrgId = UUID.randomUUID();
        adopterId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("PetRepository")
    class PetRepositoryTests {

        @Test
        @DisplayName("should find pet by microchip ID")
        void shouldFindPetByMicrochipId() {
            // Given
            Pet pet = createAndSavePet("CHIP123456", PetStatus.DRAFT);

            // When
            Optional<Pet> found = petRepository.findByMicrochipId("CHIP123456");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("should return empty when microchip not found")
        void shouldReturnEmptyWhenMicrochipNotFound() {
            // When
            Optional<Pet> found = petRepository.findByMicrochipId("NONEXISTENT");

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should find all pets by status")
        void shouldFindAllPetsByStatus() {
            // Given
            createAndSavePet("CHIP1", PetStatus.AVAILABLE);
            createAndSavePet("CHIP2", PetStatus.AVAILABLE);
            createAndSavePet("CHIP3", PetStatus.DRAFT);

            // When
            List<Pet> availablePets = petRepository.findByStatus(PetStatus.AVAILABLE);

            // Then
            assertThat(availablePets).hasSize(2);
        }

        @Test
        @DisplayName("should find pending pets by rescue org ID")
        void shouldFindPendingPetsByRescueOrgId() {
            // Given
            Pet pet1 = createAndSavePetWithRescueOrg("CHIP1", PetStatus.PENDING_RESCUE, rescueOrgId);
            Pet pet2 = createAndSavePetWithRescueOrg("CHIP2", PetStatus.PENDING_RESCUE, rescueOrgId);
            Pet pet3 = createAndSavePetWithRescueOrg("CHIP3", PetStatus.AVAILABLE, rescueOrgId);
            Pet pet4 = createAndSavePetWithRescueOrg("CHIP4", PetStatus.PENDING_RESCUE, UUID.randomUUID()); // Different rescue

            // When
            List<Pet> pendingPets = petRepository.findPendingByRescueOrgId(rescueOrgId);

            // Then
            assertThat(pendingPets).hasSize(2);
            assertThat(pendingPets).allMatch(p -> p.getStatus() == PetStatus.PENDING_RESCUE);
            assertThat(pendingPets).allMatch(p -> p.getRescueOrgId().equals(rescueOrgId));
        }

        @Test
        @DisplayName("should count pets by status")
        void shouldCountPetsByStatus() {
            // Given
            createAndSavePet("CHIP1", PetStatus.AVAILABLE);
            createAndSavePet("CHIP2", PetStatus.AVAILABLE);
            createAndSavePet("CHIP3", PetStatus.AVAILABLE);
            createAndSavePet("CHIP4", PetStatus.DRAFT);

            // When
            long count = petRepository.countByStatus(PetStatus.AVAILABLE);

            // Then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("should find featured pets with limit")
        void shouldFindFeaturedPetsWithLimit() {
            // Given
            for (int i = 0; i < 10; i++) {
                createAndSavePet("CHIP" + i, PetStatus.AVAILABLE);
            }

            // When
            List<Pet> featured = petRepository.findFeaturedPets(5);

            // Then
            assertThat(featured).hasSize(5);
        }

        @Test
        @DisplayName("should find available pets with species filter")
        void shouldFindAvailablePetsWithSpeciesFilter() {
            // Given
            createAndSavePetWithSpecies("CHIP1", Species.DOG, PetStatus.AVAILABLE);
            createAndSavePetWithSpecies("CHIP2", Species.DOG, PetStatus.AVAILABLE);
            createAndSavePetWithSpecies("CHIP3", Species.CAT, PetStatus.AVAILABLE);

            // When
            List<Pet> dogs = petRepository.findAvailableWithFilters("DOG", null, null, null, null);

            // Then
            assertThat(dogs).hasSize(2);
            assertThat(dogs).allMatch(p -> p.getSpecies() == Species.DOG);
        }

        @Test
        @DisplayName("should find available pets paginated")
        void shouldFindAvailablePetsPaginated() {
            // Given
            for (int i = 0; i < 10; i++) {
                createAndSavePet("CHIP" + i, PetStatus.AVAILABLE);
            }

            // When - Page 1
            List<Pet> page1 = petRepository.findAvailableWithFiltersPageable(null, null, null, null, null, 3, 0);
            // Page 2
            List<Pet> page2 = petRepository.findAvailableWithFiltersPageable(null, null, null, null, null, 3, 3);

            // Then
            assertThat(page1).hasSize(3);
            assertThat(page2).hasSize(3);
        }

        @Test
        @DisplayName("should count available pets with filters")
        void shouldCountAvailablePetsWithFilters() {
            // Given
            createAndSavePetWithSpecies("CHIP1", Species.DOG, PetStatus.AVAILABLE);
            createAndSavePetWithSpecies("CHIP2", Species.DOG, PetStatus.AVAILABLE);
            createAndSavePetWithSpecies("CHIP3", Species.CAT, PetStatus.AVAILABLE);
            createAndSavePetWithSpecies("CHIP4", Species.DOG, PetStatus.DRAFT); // Not available

            // When
            long count = petRepository.countAvailableWithFilters("DOG", null, null, null, null);

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("AdoptionApplicationRepository")
    class AdoptionApplicationRepositoryTests {

        @Test
        @DisplayName("should find applications by pet ID")
        void shouldFindApplicationsByPetId() {
            // Given
            Pet pet = createAndSavePet("CHIP1", PetStatus.AVAILABLE);
            UUID petId = pet.getId();

            AdoptionApplication app1 = AdoptionApplication.create(petId, adopterId, "I love dogs");
            AdoptionApplication app2 = AdoptionApplication.create(petId, UUID.randomUUID(), "Me too");
            applicationRepository.save(app1);
            applicationRepository.save(app2);

            // When
            List<AdoptionApplication> applications = applicationRepository.findByPetId(petId);

            // Then
            assertThat(applications).hasSize(2);
        }

        @Test
        @DisplayName("should find application by pet ID and adopter ID")
        void shouldFindApplicationByPetIdAndAdopterId() {
            // Given
            Pet pet = createAndSavePet("CHIP1", PetStatus.AVAILABLE);
            UUID petId = pet.getId();

            AdoptionApplication app = AdoptionApplication.create(petId, adopterId, "Please approve");
            applicationRepository.save(app);

            // When
            Optional<AdoptionApplication> found = applicationRepository.findByPetIdAndAdopterId(petId, adopterId);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getWhyAdopt()).isEqualTo("Please approve");
        }

        @Test
        @DisplayName("should find only active applications by pet ID")
        void shouldFindOnlyActiveApplicationsByPetId() {
            // Given
            Pet pet = createAndSavePet("CHIP1", PetStatus.AVAILABLE);
            UUID petId = pet.getId();

            AdoptionApplication submitted = AdoptionApplication.create(petId, UUID.randomUUID(), "Submitted");
            AdoptionApplication underReview = AdoptionApplication.create(petId, UUID.randomUUID(), "Under review");
            // Set underReview to UNDER_REVIEW using reflection
            setApplicationStatus(underReview, ApplicationStatus.UNDER_REVIEW);

            AdoptionApplication rejected = AdoptionApplication.create(petId, UUID.randomUUID(), "Rejected");
            setApplicationStatus(rejected, ApplicationStatus.REJECTED);

            applicationRepository.save(submitted);
            applicationRepository.save(underReview);
            applicationRepository.save(rejected);

            // When
            List<AdoptionApplication> activeApps = applicationRepository.findActiveByPetId(petId);

            // Then
            assertThat(activeApps).hasSize(2); // Only SUBMITTED and UNDER_REVIEW
        }

        @Test
        @DisplayName("should count active applications by adopter ID")
        void shouldCountActiveApplicationsByAdopterId() {
            // Given
            Pet pet1 = createAndSavePet("CHIP1", PetStatus.AVAILABLE);
            Pet pet2 = createAndSavePet("CHIP2", PetStatus.AVAILABLE);
            Pet pet3 = createAndSavePet("CHIP3", PetStatus.AVAILABLE);

            AdoptionApplication app1 = AdoptionApplication.create(pet1.getId(), adopterId, "App 1");
            AdoptionApplication app2 = AdoptionApplication.create(pet2.getId(), adopterId, "App 2");
            AdoptionApplication app3 = AdoptionApplication.create(pet3.getId(), adopterId, "App 3");
            setApplicationStatus(app3, ApplicationStatus.REJECTED); // Should not be counted

            applicationRepository.save(app1);
            applicationRepository.save(app2);
            applicationRepository.save(app3);

            // When
            int count = applicationRepository.countActiveByAdopterId(adopterId);

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("UserRepository")
    class UserRepositoryTests {

        @Test
        @DisplayName("should find user by email")
        void shouldFindUserByEmail() {
            // Given
            User user = User.create("test@example.com", "hashedPassword", UserRole.ADOPTER);
            userRepository.save(user);

            // When
            Optional<User> found = userRepository.findByEmail("test@example.com");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getRole()).isEqualTo(UserRole.ADOPTER);
        }

        @Test
        @DisplayName("should find users by role and status")
        void shouldFindUsersByRoleAndStatus() {
            // Given
            User pendingRescue1 = User.create("rescue1@example.com", "hash", UserRole.RESCUE_ORG);
            User pendingRescue2 = User.create("rescue2@example.com", "hash", UserRole.RESCUE_ORG);
            User activeAdopter = User.create("adopter@example.com", "hash", UserRole.ADOPTER);
            activateUser(activeAdopter);

            userRepository.save(pendingRescue1);
            userRepository.save(pendingRescue2);
            userRepository.save(activeAdopter);

            // When
            List<User> pendingRescues = userRepository.findByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING);

            // Then
            assertThat(pendingRescues).hasSize(2);
        }

        @Test
        @DisplayName("should count users by role")
        void shouldCountUsersByRole() {
            // Given
            userRepository.save(User.create("adopter1@example.com", "hash", UserRole.ADOPTER));
            userRepository.save(User.create("adopter2@example.com", "hash", UserRole.ADOPTER));
            userRepository.save(User.create("foster1@example.com", "hash", UserRole.FOSTER));

            // When
            long adopterCount = userRepository.countByRole(UserRole.ADOPTER);
            long fosterCount = userRepository.countByRole(UserRole.FOSTER);

            // Then
            assertThat(adopterCount).isEqualTo(2);
            assertThat(fosterCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should search users by name or email")
        void shouldSearchUsersByNameOrEmail() {
            // Given
            User user1 = User.create("john@example.com", "hash", UserRole.ADOPTER);
            User user2 = User.create("jane@example.com", "hash", UserRole.FOSTER);
            User user3 = User.create("bob@example.com", "hash", UserRole.ADOPTER);
            userRepository.save(user1);
            userRepository.save(user2);
            userRepository.save(user3);

            // When
            List<User> results = userRepository.searchByNameOrEmail("%john%", 10, 0);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should page users correctly")
        void shouldPageUsersCorrectly() {
            // Given
            for (int i = 0; i < 10; i++) {
                userRepository.save(User.create("user" + i + "@example.com", "hash", UserRole.ADOPTER));
            }

            // When
            List<User> page1 = userRepository.findAllPaged(3, 0);
            List<User> page2 = userRepository.findAllPaged(3, 3);

            // Then
            assertThat(page1).hasSize(3);
            assertThat(page2).hasSize(3);
        }

        @Test
        @DisplayName("should check if email exists")
        void shouldCheckIfEmailExists() {
            // Given
            userRepository.save(User.create("existing@example.com", "hash", UserRole.ADOPTER));

            // When/Then
            assertThat(userRepository.existsByEmail("existing@example.com")).isTrue();
            assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
        }
    }

    // Helper methods

    private Pet createAndSavePet(String microchipId, PetStatus status) {
        Pet pet = Pet.create(
                fosterId,
                "Buddy",
                Species.DOG,
                Breed.LABRADOR,
                3,
                AgeUnit.YEARS,
                PetSex.MALE,
                PetSize.LARGE,
                microchipId,
                "A friendly dog",
                null
        );
        setStatus(pet, status);
        return petRepository.save(pet);
    }

    private Pet createAndSavePetWithRescueOrg(String microchipId, PetStatus status, UUID rescueOrgId) {
        Pet pet = Pet.create(
                fosterId,
                "Buddy",
                Species.DOG,
                Breed.LABRADOR,
                3,
                AgeUnit.YEARS,
                PetSex.MALE,
                PetSize.LARGE,
                microchipId,
                "A friendly dog",
                null
        );
        setStatus(pet, status);
        setRescueOrgId(pet, rescueOrgId);
        return petRepository.save(pet);
    }

    private Pet createAndSavePetWithSpecies(String microchipId, Species species, PetStatus status) {
        Pet pet = Pet.create(
                fosterId,
                "Pet",
                species,
                null,
                2,
                AgeUnit.YEARS,
                PetSex.FEMALE,
                PetSize.MEDIUM,
                microchipId,
                "Description",
                null
        );
        setStatus(pet, status);
        return petRepository.save(pet);
    }

    private void setStatus(Pet pet, PetStatus status) {
        try {
            java.lang.reflect.Field statusField = Pet.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(pet, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setRescueOrgId(Pet pet, UUID rescueOrgId) {
        try {
            java.lang.reflect.Field field = Pet.class.getDeclaredField("rescueOrgId");
            field.setAccessible(true);
            field.set(pet, rescueOrgId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setApplicationStatus(AdoptionApplication app, ApplicationStatus status) {
        try {
            java.lang.reflect.Field statusField = AdoptionApplication.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(app, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void activateUser(User user) {
        try {
            java.lang.reflect.Field statusField = User.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(user, AccountStatus.ACTIVE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
