package com.example.foreverhome.controller;

import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.Breed;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.Species;
import com.example.foreverhome.domain.profile.Adopter;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.AdopterRepository;
import com.example.foreverhome.repository.AdoptionApplicationRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RescueDashboardController")
class RescueDashboardControllerTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetImageRepository petImageRepository;

    @Mock
    private AdoptionApplicationRepository applicationRepository;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private S3StorageService storageService;

    @InjectMocks
    private RescueDashboardController rescueDashboardController;

    private UserPrincipal rescuePrincipal;
    private UUID userId;
    private UUID rescueOrgId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        rescueOrgId = UUID.randomUUID();
        rescuePrincipal = new UserPrincipal(userId, UserRole.RESCUE_ORG, true);
    }

    @Nested
    @DisplayName("GET /api/rescues/my/pending")
    class GetPendingPets {

        @Test
        @DisplayName("given rescue org with pending pets, when getPendingPets, then returns list of pending pets")
        void givenRescueOrgWithPendingPets_whenGetPendingPets_thenReturnsListOfPendingPets() {
            // Given
            RescueOrganization rescueOrg = createRescueOrg(rescueOrgId, userId);
            Pet pet1 = createPet("Buddy", rescueOrgId);
            Pet pet2 = createPet("Max", rescueOrgId);

            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));
            when(petRepository.findPendingByRescueOrgId(rescueOrgId)).thenReturn(List.of(pet1, pet2));
            when(petImageRepository.findByPetIdOrderByDisplayOrder(any())).thenReturn(List.of());

            // When
            ResponseEntity<List<PetDto>> response = rescueDashboardController.getPendingPets(rescuePrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("given rescue org not found, when getPendingPets, then throws ResourceNotFoundException")
        void givenRescueOrgNotFound_whenGetPendingPets_thenThrowsResourceNotFoundException() {
            // Given
            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> rescueDashboardController.getPendingPets(rescuePrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GET /api/rescues/my/pets")
    class GetAllPets {

        @Test
        @DisplayName("given rescue org with pets, when getAllPets, then returns list of all pets")
        void givenRescueOrgWithPets_whenGetAllPets_thenReturnsListOfAllPets() {
            // Given
            RescueOrganization rescueOrg = createRescueOrg(rescueOrgId, userId);
            Pet pet = createPet("Buddy", rescueOrgId);

            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));
            when(petRepository.findByRescueOrgId(rescueOrgId)).thenReturn(List.of(pet));
            when(petImageRepository.findByPetIdOrderByDisplayOrder(any())).thenReturn(List.of());

            // When
            ResponseEntity<List<PetDto>> response = rescueDashboardController.getAllPets(rescuePrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).name()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("given rescue org with no pets, when getAllPets, then returns empty list")
        void givenRescueOrgWithNoPets_whenGetAllPets_thenReturnsEmptyList() {
            // Given
            RescueOrganization rescueOrg = createRescueOrg(rescueOrgId, userId);

            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));
            when(petRepository.findByRescueOrgId(rescueOrgId)).thenReturn(List.of());

            // When
            ResponseEntity<List<PetDto>> response = rescueDashboardController.getAllPets(rescuePrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/rescues/my/applications")
    class GetApplications {

        @Test
        @DisplayName("given rescue org with applications, when getApplications, then returns list of applications")
        void givenRescueOrgWithApplications_whenGetApplications_thenReturnsListOfApplications() {
            // Given
            RescueOrganization rescueOrg = createRescueOrg(rescueOrgId, userId);
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            Pet pet = createPet("Buddy", rescueOrgId);
            setId(pet, petId);
            Adopter adopter = createAdopter(adopterId, UUID.randomUUID());
            AdoptionApplication application = createApplication(petId, adopterId);

            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));
            when(petRepository.findByRescueOrgId(rescueOrgId)).thenReturn(List.of(pet));
            when(applicationRepository.findByPetId(petId)).thenReturn(List.of(application));
            when(adopterRepository.findAllByIds(List.of(adopterId))).thenReturn(List.of(adopter));

            // When
            ResponseEntity<List<RescueDashboardController.ApplicationResponse>> response =
                    rescueDashboardController.getApplications(rescuePrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).petName()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("given rescue org with no pets, when getApplications, then returns empty list")
        void givenRescueOrgWithNoPets_whenGetApplications_thenReturnsEmptyList() {
            // Given
            RescueOrganization rescueOrg = createRescueOrg(rescueOrgId, userId);

            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));
            when(petRepository.findByRescueOrgId(rescueOrgId)).thenReturn(List.of());

            // When
            ResponseEntity<List<RescueDashboardController.ApplicationResponse>> response =
                    rescueDashboardController.getApplications(rescuePrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    // Helper methods
    private RescueOrganization createRescueOrg(UUID id, UUID userId) {
        RescueOrganization rescueOrg = RescueOrganization.create(userId, "Happy Tails Rescue", "123-456-7890",
                "https://happytails.org", "A great rescue", "John Doe", "contact@happytails.org", null, null);
        setId(rescueOrg, id);
        return rescueOrg;
    }

    private Pet createPet(String name, UUID rescueOrgId) {
        Pet pet = Pet.create(UUID.randomUUID(), name, Species.DOG, Breed.LABRADOR, 2, AgeUnit.YEARS,
                PetSex.MALE, PetSize.LARGE, "MICRO123", "A friendly dog", "Healthy");
        setId(pet, UUID.randomUUID());
        return pet;
    }

    private Adopter createAdopter(UUID id, UUID userId) {
        Adopter adopter = Adopter.create(userId, "John", "Doe", "555-1234", "House", "5 years", null);
        setId(adopter, id);
        return adopter;
    }

    private AdoptionApplication createApplication(UUID petId, UUID adopterId) {
        AdoptionApplication application = AdoptionApplication.create(petId, adopterId, "House", "5 years", "Love dogs");
        setId(application, UUID.randomUUID());
        return application;
    }

    private void setId(Object entity, UUID id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }
}
