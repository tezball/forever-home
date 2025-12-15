package com.example.foreverhome.controller;

import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.pet.Species;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.repository.AdoptionRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.VetRepository;
import com.example.foreverhome.repository.VetSignOffRepository;
import com.example.foreverhome.service.S3StorageService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicController")
class PublicControllerTest {

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetImageRepository petImageRepository;

    @Mock
    private AdoptionRepository adoptionRepository;

    @Mock
    private VetRepository vetRepository;

    @Mock
    private VetSignOffRepository vetSignOffRepository;

    @Mock
    private S3StorageService storageService;

    @InjectMocks
    private PublicController publicController;

    @Nested
    @DisplayName("GET /api/rescues")
    class ListRescueOrganizations {

        @Test
        @DisplayName("given verified rescue organizations exist, when listRescueOrganizations, then returns list")
        void givenVerifiedRescueOrganizationsExist_whenListRescueOrganizations_thenReturnsList() {
            // Given
            RescueOrganization org1 = createRescueOrg("Happy Tails", true);
            RescueOrganization org2 = createRescueOrg("Paws & Love", true);
            UUID org1Id = UUID.randomUUID();
            UUID org2Id = UUID.randomUUID();
            setId(org1, org1Id);
            setId(org2, org2Id);

            when(rescueOrganizationRepository.findAllVerified()).thenReturn(List.of(org1, org2));
            when(petRepository.countByRescueOrgIdAndStatus(org1Id, PetStatus.AVAILABLE)).thenReturn(5L);
            when(petRepository.countByRescueOrgIdAndStatus(org2Id, PetStatus.AVAILABLE)).thenReturn(3L);

            // When
            ResponseEntity<List<PublicController.RescueOrgPublicResponse>> response =
                    publicController.listRescueOrganizations();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).name()).isEqualTo("Happy Tails");
            assertThat(response.getBody().get(0).petCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("given no verified rescue organizations, when listRescueOrganizations, then returns empty list")
        void givenNoVerifiedRescueOrganizations_whenListRescueOrganizations_thenReturnsEmptyList() {
            // Given
            when(rescueOrganizationRepository.findAllVerified()).thenReturn(List.of());

            // When
            ResponseEntity<List<PublicController.RescueOrgPublicResponse>> response =
                    publicController.listRescueOrganizations();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/rescues/{id}")
    class GetRescueOrganization {

        @Test
        @DisplayName("given verified rescue organization, when getRescueOrganization, then returns organization")
        void givenVerifiedRescueOrganization_whenGetRescueOrganization_thenReturnsOrganization() {
            // Given
            UUID orgId = UUID.randomUUID();
            RescueOrganization org = createRescueOrg("Happy Tails", true);
            setId(org, orgId);

            when(rescueOrganizationRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(petRepository.countByRescueOrgIdAndStatus(orgId, PetStatus.AVAILABLE)).thenReturn(10L);

            // When
            ResponseEntity<PublicController.RescueOrgPublicResponse> response =
                    publicController.getRescueOrganization(orgId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().name()).isEqualTo("Happy Tails");
            assertThat(response.getBody().petCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("given non-existent rescue organization, when getRescueOrganization, then returns not found")
        void givenNonExistentRescueOrganization_whenGetRescueOrganization_thenReturnsNotFound() {
            // Given
            UUID orgId = UUID.randomUUID();
            when(rescueOrganizationRepository.findById(orgId)).thenReturn(Optional.empty());

            // When
            ResponseEntity<PublicController.RescueOrgPublicResponse> response =
                    publicController.getRescueOrganization(orgId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("given unverified rescue organization, when getRescueOrganization, then returns not found")
        void givenUnverifiedRescueOrganization_whenGetRescueOrganization_thenReturnsNotFound() {
            // Given
            UUID orgId = UUID.randomUUID();
            RescueOrganization org = createRescueOrg("Unverified Org", false);
            setId(org, orgId);

            when(rescueOrganizationRepository.findById(orgId)).thenReturn(Optional.of(org));

            // When
            ResponseEntity<PublicController.RescueOrgPublicResponse> response =
                    publicController.getRescueOrganization(orgId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/rescues/{id}/pets")
    class GetRescuePets {

        @Test
        @DisplayName("given rescue org with available pets, when getRescuePets, then returns list of pets")
        void givenRescueOrgWithAvailablePets_whenGetRescuePets_thenReturnsListOfPets() {
            // Given
            UUID orgId = UUID.randomUUID();
            RescueOrganization org = createRescueOrg("Happy Tails", true);
            setId(org, orgId);
            Pet pet = createPet("Buddy", orgId);
            UUID petId = UUID.randomUUID();
            setId(pet, petId);

            when(rescueOrganizationRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(petRepository.findByRescueOrgIdAndStatus(orgId, PetStatus.AVAILABLE)).thenReturn(List.of(pet));
            when(petImageRepository.findByPetIdOrderByDisplayOrder(petId)).thenReturn(List.of());

            // When
            ResponseEntity<List<PublicController.PetPublicResponse>> response =
                    publicController.getRescuePets(orgId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).name()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("given non-existent rescue org, when getRescuePets, then returns not found")
        void givenNonExistentRescueOrg_whenGetRescuePets_thenReturnsNotFound() {
            // Given
            UUID orgId = UUID.randomUUID();
            when(rescueOrganizationRepository.findById(orgId)).thenReturn(Optional.empty());

            // When
            ResponseEntity<List<PublicController.PetPublicResponse>> response =
                    publicController.getRescuePets(orgId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/vets")
    class ListVets {

        @Test
        @DisplayName("given verified vets exist, when listVets, then returns list")
        void givenVerifiedVetsExist_whenListVets_thenReturnsList() {
            // Given
            Vet vet = createVet("Happy Paws Clinic", true);
            UUID vetId = UUID.randomUUID();
            setId(vet, vetId);

            when(vetRepository.findAllVerified()).thenReturn(List.of(vet));
            when(vetSignOffRepository.countByVetId(vetId)).thenReturn(25L);

            // When
            ResponseEntity<List<PublicController.VetPublicResponse>> response = publicController.listVets();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).clinicName()).isEqualTo("Happy Paws Clinic");
            assertThat(response.getBody().get(0).signOffCount()).isEqualTo(25L);
        }

        @Test
        @DisplayName("given no verified vets, when listVets, then returns empty list")
        void givenNoVerifiedVets_whenListVets_thenReturnsEmptyList() {
            // Given
            when(vetRepository.findAllVerified()).thenReturn(List.of());

            // When
            ResponseEntity<List<PublicController.VetPublicResponse>> response = publicController.listVets();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/vets/{id}")
    class GetVet {

        @Test
        @DisplayName("given verified vet, when getVet, then returns vet")
        void givenVerifiedVet_whenGetVet_thenReturnsVet() {
            // Given
            UUID vetId = UUID.randomUUID();
            Vet vet = createVet("Happy Paws Clinic", true);
            setId(vet, vetId);

            when(vetRepository.findById(vetId)).thenReturn(Optional.of(vet));
            when(vetSignOffRepository.countByVetId(vetId)).thenReturn(50L);

            // When
            ResponseEntity<PublicController.VetPublicResponse> response = publicController.getVet(vetId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().clinicName()).isEqualTo("Happy Paws Clinic");
            assertThat(response.getBody().signOffCount()).isEqualTo(50L);
        }

        @Test
        @DisplayName("given non-existent vet, when getVet, then returns not found")
        void givenNonExistentVet_whenGetVet_thenReturnsNotFound() {
            // Given
            UUID vetId = UUID.randomUUID();
            when(vetRepository.findById(vetId)).thenReturn(Optional.empty());

            // When
            ResponseEntity<PublicController.VetPublicResponse> response = publicController.getVet(vetId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("given unverified vet, when getVet, then returns not found")
        void givenUnverifiedVet_whenGetVet_thenReturnsNotFound() {
            // Given
            UUID vetId = UUID.randomUUID();
            Vet vet = createVet("Unverified Clinic", false);
            setId(vet, vetId);

            when(vetRepository.findById(vetId)).thenReturn(Optional.of(vet));

            // When
            ResponseEntity<PublicController.VetPublicResponse> response = publicController.getVet(vetId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/stats")
    class GetPlatformStats {

        @Test
        @DisplayName("given platform data, when getPlatformStats, then returns stats")
        void givenPlatformData_whenGetPlatformStats_thenReturnsStats() {
            // Given
            when(petRepository.countByStatus(PetStatus.AVAILABLE)).thenReturn(100L);
            when(adoptionRepository.countAll()).thenReturn(500L);
            when(rescueOrganizationRepository.findAllVerified()).thenReturn(
                    List.of(createRescueOrg("Org1", true), createRescueOrg("Org2", true))
            );

            // When
            ResponseEntity<PublicController.PlatformStatsResponse> response = publicController.getPlatformStats();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().petsAvailable()).isEqualTo(100L);
            assertThat(response.getBody().totalAdoptions()).isEqualTo(500L);
            assertThat(response.getBody().totalRescues()).isEqualTo(2L);
        }

        @Test
        @DisplayName("given empty platform, when getPlatformStats, then returns zero stats")
        void givenEmptyPlatform_whenGetPlatformStats_thenReturnsZeroStats() {
            // Given
            when(petRepository.countByStatus(PetStatus.AVAILABLE)).thenReturn(0L);
            when(adoptionRepository.countAll()).thenReturn(0L);
            when(rescueOrganizationRepository.findAllVerified()).thenReturn(List.of());

            // When
            ResponseEntity<PublicController.PlatformStatsResponse> response = publicController.getPlatformStats();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().petsAvailable()).isEqualTo(0L);
            assertThat(response.getBody().totalAdoptions()).isEqualTo(0L);
            assertThat(response.getBody().totalRescues()).isEqualTo(0L);
        }
    }

    // Helper methods
    private RescueOrganization createRescueOrg(String name, boolean verified) {
        RescueOrganization org = RescueOrganization.create(UUID.randomUUID(), name, "123-456-7890",
                "https://" + name.toLowerCase().replace(" ", "") + ".org", "A great rescue", "John Doe",
                "contact@" + name.toLowerCase().replace(" ", "") + ".org", null, null);
        if (verified) {
            org.verify();
        }
        setId(org, UUID.randomUUID());
        return org;
    }

    private Pet createPet(String name, UUID rescueOrgId) {
        Pet pet = Pet.create(UUID.randomUUID(), name, Species.DOG, "Labrador", 2, AgeUnit.YEARS,
                PetSex.MALE, PetSize.LARGE, "MICRO123", "A friendly dog", "Healthy");
        setId(pet, UUID.randomUUID());
        return pet;
    }

    private Vet createVet(String clinicName, boolean verified) {
        Vet vet = Vet.create(UUID.randomUUID(), clinicName, "VET001", "555-1234", null, null, null);
        if (verified) {
            vet.verify();
        }
        setId(vet, UUID.randomUUID());
        return vet;
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
