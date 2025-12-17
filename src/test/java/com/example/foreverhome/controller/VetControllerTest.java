package com.example.foreverhome.controller;

import com.example.foreverhome.domain.pet.Breed;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.Species;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.domain.verification.VetApprovalRequest;
import com.example.foreverhome.domain.verification.HealthStatus;
import com.example.foreverhome.domain.verification.VetSignOff;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.VetRepository;
import com.example.foreverhome.repository.VetSignOffRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.PetService;
import com.example.foreverhome.service.VetApprovalRequestService;
import com.example.foreverhome.service.VetApprovalService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VetController")
class VetControllerTest {

    @Mock
    private PetService petService;

    @Mock
    private VetApprovalService vetApprovalService;

    @Mock
    private VetApprovalRequestService vetApprovalRequestService;

    @Mock
    private VetRepository vetRepository;

    @Mock
    private VetSignOffRepository vetSignOffRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @InjectMocks
    private VetController vetController;

    private UserPrincipal vetPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        vetPrincipal = new UserPrincipal(userId, UserRole.VET, true);
    }

    @Nested
    @DisplayName("GET /api/vet/pets/pending")
    class GetPendingPets {

        @Test
        @DisplayName("given vet with pending pets, when getPendingPets, then returns list of pets")
        void givenVetWithPendingPets_whenGetPendingPets_thenReturnsListOfPets() {
            // Given
            PetDto pet1 = createPetDto("Buddy");
            PetDto pet2 = createPetDto("Max");
            when(petService.getPendingVetPetsForVet(userId)).thenReturn(List.of(pet1, pet2));

            // When
            ResponseEntity<List<PetDto>> response = vetController.getPendingPets(vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("given vet with no pending pets, when getPendingPets, then returns empty list")
        void givenVetWithNoPendingPets_whenGetPendingPets_thenReturnsEmptyList() {
            // Given
            when(petService.getPendingVetPetsForVet(userId)).thenReturn(List.of());

            // When
            ResponseEntity<List<PetDto>> response = vetController.getPendingPets(vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/vet/pets/lookup")
    class LookupByMicrochip {

        @Test
        @DisplayName("given valid microchip, when lookupByMicrochip, then returns pet")
        void givenValidMicrochip_whenLookupByMicrochip_thenReturnsPet() {
            // Given
            String microchip = "MICRO123";
            PetDto petDto = createPetDto("Buddy");
            when(petService.findByMicrochipForVet(microchip, userId)).thenReturn(petDto);

            // When
            ResponseEntity<PetDto> response = vetController.lookupByMicrochip(microchip, vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().name()).isEqualTo("Buddy");
        }
    }

    @Nested
    @DisplayName("POST /api/vet/pets/{petId}/sign-off")
    class SignOffPet {

        @Test
        @DisplayName("given valid pet and sign-off request, when signOffPet, then returns updated pet")
        void givenValidPetAndSignOffRequest_whenSignOffPet_thenReturnsUpdatedPet() {
            // Given
            UUID petId = UUID.randomUUID();
            VetController.VetSignOffRequest request = new VetController.VetSignOffRequest(true, true, true, "Healthy");
            PetDto petDto = createPetDto("Buddy");
            when(petService.signOffByVetWithValidation(petId, userId, request)).thenReturn(petDto);

            // When
            ResponseEntity<PetDto> response = vetController.signOffPet(petId, request, vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(petService).signOffByVetWithValidation(petId, userId, request);
        }
    }

    @Nested
    @DisplayName("POST /api/vet/pets/{petId}/decline")
    class DeclinePet {

        @Test
        @DisplayName("given valid pet and decline request, when declinePet, then returns updated pet")
        void givenValidPetAndDeclineRequest_whenDeclinePet_thenReturnsUpdatedPet() {
            // Given
            UUID petId = UUID.randomUUID();
            VetController.VetDeclineRequest request = new VetController.VetDeclineRequest("Not healthy");
            PetDto petDto = createPetDto("Buddy");
            when(petService.declineByVetWithValidation(petId, userId, "Not healthy")).thenReturn(petDto);

            // When
            ResponseEntity<PetDto> response = vetController.declinePet(petId, request, vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(petService).declineByVetWithValidation(petId, userId, "Not healthy");
        }
    }

    @Nested
    @DisplayName("GET /api/vet/rescue-orgs")
    class GetMyRescueOrgs {

        @Test
        @DisplayName("given vet with approved rescue orgs, when getMyRescueOrgs, then returns list")
        void givenVetWithApprovedRescueOrgs_whenGetMyRescueOrgs_thenReturnsList() {
            // Given
            RescueOrganization org = createRescueOrg("Happy Tails");
            when(vetApprovalService.getRescueOrgsForVet(userId)).thenReturn(List.of(org));

            // When
            ResponseEntity<List<VetController.RescueOrgSummary>> response = vetController.getMyRescueOrgs(vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).name()).isEqualTo("Happy Tails");
        }
    }

    @Nested
    @DisplayName("GET /api/vet/rescue-orgs/available")
    class GetAvailableRescueOrgs {

        @Test
        @DisplayName("given available rescue orgs, when getAvailableRescueOrgs, then returns list")
        void givenAvailableRescueOrgs_whenGetAvailableRescueOrgs_thenReturnsList() {
            // Given
            RescueOrganization org = createRescueOrg("New Rescue");
            when(vetApprovalRequestService.getAvailableRescueOrgsForVet(userId)).thenReturn(List.of(org));

            // When
            ResponseEntity<List<VetController.RescueOrgSummary>> response = vetController.getAvailableRescueOrgs(vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("POST /api/vet/rescue-orgs/{rescueOrgId}/request")
    class RequestApproval {

        @Test
        @DisplayName("given valid rescue org ID, when requestApproval, then returns approval request")
        void givenValidRescueOrgId_whenRequestApproval_thenReturnsApprovalRequest() {
            // Given
            UUID rescueOrgId = UUID.randomUUID();
            UUID vetId = UUID.randomUUID();
            VetApprovalRequest request = VetApprovalRequest.create(vetId, rescueOrgId, "Please approve");
            setId(request, UUID.randomUUID());
            RescueOrganization org = createRescueOrg("Happy Tails");
            setId(org, rescueOrgId);

            VetController.ApprovalRequestBody requestBody = new VetController.ApprovalRequestBody("Please approve");
            when(vetApprovalRequestService.requestApproval(userId, rescueOrgId, "Please approve")).thenReturn(request);
            when(rescueOrganizationRepository.findById(rescueOrgId)).thenReturn(Optional.of(org));

            // When
            ResponseEntity<VetController.ApprovalRequestResponse> response =
                    vetController.requestApproval(rescueOrgId, requestBody, vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().rescueOrgName()).isEqualTo("Happy Tails");
        }
    }

    @Nested
    @DisplayName("GET /api/vet/approval-requests")
    class GetMyApprovalRequests {

        @Test
        @DisplayName("given vet with approval requests, when getMyApprovalRequests, then returns list")
        void givenVetWithApprovalRequests_whenGetMyApprovalRequests_thenReturnsList() {
            // Given
            UUID rescueOrgId = UUID.randomUUID();
            UUID vetId = UUID.randomUUID();
            VetApprovalRequest request = VetApprovalRequest.create(vetId, rescueOrgId, "Please approve");
            setId(request, UUID.randomUUID());
            RescueOrganization org = createRescueOrg("Happy Tails");
            setId(org, rescueOrgId);

            when(vetApprovalRequestService.getRequestsForVet(userId)).thenReturn(List.of(request));
            when(rescueOrganizationRepository.findById(rescueOrgId)).thenReturn(Optional.of(org));

            // When
            ResponseEntity<List<VetController.ApprovalRequestResponse>> response =
                    vetController.getMyApprovalRequests(vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("DELETE /api/vet/approval-requests/{requestId}")
    class WithdrawApprovalRequest {

        @Test
        @DisplayName("given valid request ID, when withdrawApprovalRequest, then returns no content")
        void givenValidRequestId_whenWithdrawApprovalRequest_thenReturnsNoContent() {
            // Given
            UUID requestId = UUID.randomUUID();
            doNothing().when(vetApprovalRequestService).withdrawRequest(userId, requestId);

            // When
            ResponseEntity<Void> response = vetController.withdrawApprovalRequest(requestId, vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(vetApprovalRequestService).withdrawRequest(userId, requestId);
        }
    }

    @Nested
    @DisplayName("GET /api/vet/sign-offs")
    class GetSignOffHistory {

        @Test
        @DisplayName("given vet with sign-offs, when getSignOffHistory, then returns list")
        void givenVetWithSignOffs_whenGetSignOffHistory_thenReturnsList() {
            // Given
            UUID vetId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            Vet vet = createVet(vetId, userId);
            Pet pet = createPet(petId, "Buddy");
            VetSignOff signOff = VetSignOff.create(petId, vetId, LocalDate.now(), HealthStatus.GOOD, "Good condition");
            setId(signOff, UUID.randomUUID());

            when(vetRepository.findByUserId(userId)).thenReturn(Optional.of(vet));
            when(vetSignOffRepository.findByVetIdOrderBySignedOffAtDesc(vetId)).thenReturn(List.of(signOff));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When
            ResponseEntity<List<VetController.SignOffHistoryItem>> response = vetController.getSignOffHistory(vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).petName()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("given vet profile not found, when getSignOffHistory, then throws ResourceNotFoundException")
        void givenVetProfileNotFound_whenGetSignOffHistory_thenThrowsResourceNotFoundException() {
            // Given
            when(vetRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> vetController.getSignOffHistory(vetPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GET /api/vet/sign-offs/count")
    class GetSignOffCount {

        @Test
        @DisplayName("given vet with sign-offs, when getSignOffCount, then returns count")
        void givenVetWithSignOffs_whenGetSignOffCount_thenReturnsCount() {
            // Given
            UUID vetId = UUID.randomUUID();
            Vet vet = createVet(vetId, userId);

            when(vetRepository.findByUserId(userId)).thenReturn(Optional.of(vet));
            when(vetSignOffRepository.countByVetId(vetId)).thenReturn(10L);

            // When
            ResponseEntity<VetController.SignOffCountResponse> response = vetController.getSignOffCount(vetPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().count()).isEqualTo(10L);
        }
    }

    // Helper methods
    private PetDto createPetDto(String name) {
        return new PetDto(
                UUID.randomUUID(),
                name,
                Species.DOG,
                Breed.LABRADOR,
                2,
                AgeUnit.YEARS,
                PetSex.MALE,
                PetSize.LARGE,
                "MICRO123",
                "A friendly dog",
                "Healthy",
                com.example.foreverhome.domain.pet.PetStatus.PENDING_VET,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                List.of(),
                true
        );
    }

    private RescueOrganization createRescueOrg(String name) {
        RescueOrganization org = RescueOrganization.create(UUID.randomUUID(), name, "123-456-7890",
                "https://rescue.org", "A great rescue", "Jane Doe", "contact@rescue.org", null, null);
        setId(org, UUID.randomUUID());
        return org;
    }

    private Vet createVet(UUID vetId, UUID userId) {
        Vet vet = Vet.create(userId, "Happy Paws Clinic", "VET001", "555-1234", null, null, null);
        setId(vet, vetId);
        return vet;
    }

    private Pet createPet(UUID petId, String name) {
        Pet pet = Pet.create(UUID.randomUUID(), name, Species.DOG, Breed.LABRADOR, 2, AgeUnit.YEARS,
                PetSex.MALE, PetSize.LARGE, "MICRO123", "A friendly dog", "Healthy");
        setId(pet, petId);
        return pet;
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
