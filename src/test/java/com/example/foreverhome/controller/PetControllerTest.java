package com.example.foreverhome.controller;

import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.pet.Species;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.pet.CreatePetRequest;
import com.example.foreverhome.dto.pet.DeclinePetRequest;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.dto.pet.SubmitForReviewRequest;
import com.example.foreverhome.dto.pet.UpdatePetRequest;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.PetService;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PetController")
class PetControllerTest {

    @Mock
    private PetService petService;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @InjectMocks
    private PetController petController;

    private UUID userId;
    private UUID petId;
    private UUID rescueOrgId;
    private UserPrincipal fosterPrincipal;
    private PetDto samplePetDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        petId = UUID.randomUUID();
        rescueOrgId = UUID.randomUUID();
        fosterPrincipal = new UserPrincipal(userId, UserRole.FOSTER, true);

        samplePetDto = new PetDto(
                petId,
                "Buddy",
                Species.DOG,
                "Labrador",
                3,
                AgeUnit.YEARS,
                PetSex.MALE,
                PetSize.LARGE,
                "MC123456789",
                "A friendly dog",
                null, // healthNotes
                PetStatus.DRAFT,
                userId,
                null, // rescueOrgId
                Instant.now(),
                List.of("http://example.com/image1.jpg"),
                null // canSignOff
        );
    }

    @Nested
    @DisplayName("GET /api/pets")
    class GetAvailablePets {

        @Test
        @DisplayName("should return all available pets without filters")
        void shouldReturnAllAvailablePetsWithoutFilters() {
            List<PetDto> pets = List.of(samplePetDto);
            when(petService.getAvailablePets()).thenReturn(pets);

            ResponseEntity<List<PetDto>> response = petController.getAvailablePets(null, null, null, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            verify(petService).getAvailablePets();
            verify(petService, never()).getAvailablePetsWithFilters(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should return filtered pets when species filter provided")
        void shouldReturnFilteredPetsWithSpecies() {
            List<PetDto> pets = List.of(samplePetDto);
            when(petService.getAvailablePetsWithFilters("DOG", null, null, null, null)).thenReturn(pets);

            ResponseEntity<List<PetDto>> response = petController.getAvailablePets("DOG", null, null, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(petService).getAvailablePetsWithFilters("DOG", null, null, null, null);
        }

        @Test
        @DisplayName("should return filtered pets with multiple filters")
        void shouldReturnFilteredPetsWithMultipleFilters() {
            List<PetDto> pets = List.of(samplePetDto);
            when(petService.getAvailablePetsWithFilters("DOG", "LARGE", "MALE", 1, 5)).thenReturn(pets);

            ResponseEntity<List<PetDto>> response = petController.getAvailablePets("DOG", "LARGE", "MALE", 1, 5);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(petService).getAvailablePetsWithFilters("DOG", "LARGE", "MALE", 1, 5);
        }
    }

    @Nested
    @DisplayName("GET /api/pets/featured")
    class GetFeaturedPets {

        @Test
        @DisplayName("should return featured pets")
        void shouldReturnFeaturedPets() {
            List<PetDto> pets = List.of(samplePetDto);
            when(petService.getFeaturedPets()).thenReturn(pets);

            ResponseEntity<List<PetDto>> response = petController.getFeaturedPets();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            verify(petService).getFeaturedPets();
        }
    }

    @Nested
    @DisplayName("GET /api/pets/{id}")
    class GetPet {

        @Test
        @DisplayName("should return pet by id")
        void shouldReturnPetById() {
            when(petService.getPet(petId)).thenReturn(samplePetDto);

            ResponseEntity<PetDto> response = petController.getPet(petId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(samplePetDto);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException when pet not found")
        void shouldPropagateNotFoundExceptionWhenPetNotFound() {
            when(petService.getPet(petId)).thenThrow(new ResourceNotFoundException("Pet not found"));

            assertThatThrownBy(() -> petController.getPet(petId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Pet not found");
        }
    }

    @Nested
    @DisplayName("POST /api/pets")
    class CreatePet {

        @Test
        @DisplayName("should create pet with valid request")
        void shouldCreatePetWithValidRequest() {
            CreatePetRequest request = new CreatePetRequest(
                    "Buddy",
                    Species.DOG,
                    "Labrador",
                    3,
                    AgeUnit.YEARS,
                    PetSex.MALE,
                    PetSize.LARGE,
                    "MC123456789",
                    "A friendly dog",
                    null // healthNotes
            );
            when(petService.createPet(userId, request)).thenReturn(samplePetDto);

            ResponseEntity<PetDto> response = petController.createPet(fosterPrincipal, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(samplePetDto);
            verify(petService).createPet(userId, request);
        }
    }

    @Nested
    @DisplayName("PUT /api/pets/{id}")
    class UpdatePet {

        @Test
        @DisplayName("should update pet with valid request")
        void shouldUpdatePetWithValidRequest() {
            UpdatePetRequest request = new UpdatePetRequest(
                    "Buddy Updated",
                    Species.DOG,
                    "Labrador Mix",
                    4,
                    AgeUnit.YEARS,
                    PetSex.MALE,
                    PetSize.LARGE,
                    "MC123456789",
                    "Updated description",
                    null // healthNotes
            );
            PetDto updatedPet = new PetDto(
                    petId,
                    "Buddy Updated",
                    Species.DOG,
                    "Labrador Mix",
                    4,
                    AgeUnit.YEARS,
                    PetSex.MALE,
                    PetSize.LARGE,
                    "MC123456789",
                    "Updated description",
                    null,
                    PetStatus.DRAFT,
                    userId,
                    null,
                    Instant.now(),
                    List.of(),
                    null
            );
            when(petService.updatePet(petId, userId, request)).thenReturn(updatedPet);

            ResponseEntity<PetDto> response = petController.updatePet(petId, fosterPrincipal, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().name()).isEqualTo("Buddy Updated");
        }
    }

    @Nested
    @DisplayName("POST /api/pets/{id}/submit")
    class SubmitForReview {

        @Test
        @DisplayName("should submit pet for review")
        void shouldSubmitPetForReview() {
            SubmitForReviewRequest request = new SubmitForReviewRequest(rescueOrgId);
            PetDto submittedPet = new PetDto(
                    petId,
                    samplePetDto.name(),
                    samplePetDto.species(),
                    samplePetDto.breed(),
                    samplePetDto.age(),
                    samplePetDto.ageUnit(),
                    samplePetDto.sex(),
                    samplePetDto.size(),
                    samplePetDto.microchipId(),
                    samplePetDto.description(),
                    null,
                    PetStatus.PENDING_RESCUE,
                    userId,
                    rescueOrgId,
                    Instant.now(),
                    List.of(),
                    null
            );
            when(petService.submitForReview(petId, userId, rescueOrgId)).thenReturn(submittedPet);

            ResponseEntity<PetDto> response = petController.submitForReview(petId, fosterPrincipal, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo(PetStatus.PENDING_RESCUE);
        }
    }

    @Nested
    @DisplayName("POST /api/pets/{id}/accept")
    class AcceptByRescue {

        @Test
        @DisplayName("should accept pet by rescue organization")
        void shouldAcceptPetByRescue() {
            UserPrincipal rescuePrincipal = new UserPrincipal(userId, UserRole.RESCUE_ORG, true);
            RescueOrganization rescueOrg = mock(RescueOrganization.class);
            when(rescueOrg.getId()).thenReturn(rescueOrgId);
            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));

            PetDto acceptedPet = new PetDto(
                    petId,
                    samplePetDto.name(),
                    samplePetDto.species(),
                    samplePetDto.breed(),
                    samplePetDto.age(),
                    samplePetDto.ageUnit(),
                    samplePetDto.sex(),
                    samplePetDto.size(),
                    samplePetDto.microchipId(),
                    samplePetDto.description(),
                    null,
                    PetStatus.PENDING_VET,
                    userId,
                    rescueOrgId,
                    Instant.now(),
                    List.of(),
                    null
            );
            when(petService.acceptByRescue(petId, rescueOrgId, userId)).thenReturn(acceptedPet);

            ResponseEntity<PetDto> response = petController.acceptByRescue(petId, rescuePrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo(PetStatus.PENDING_VET);
        }

        @Test
        @DisplayName("should throw exception when rescue org profile not found")
        void shouldThrowExceptionWhenRescueOrgNotFound() {
            UserPrincipal rescuePrincipal = new UserPrincipal(userId, UserRole.RESCUE_ORG, true);
            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petController.acceptByRescue(petId, rescuePrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rescue organization profile not found");
        }
    }

    @Nested
    @DisplayName("POST /api/pets/{id}/decline")
    class DeclineByRescue {

        @Test
        @DisplayName("should decline pet with reason")
        void shouldDeclinePetWithReason() {
            UserPrincipal rescuePrincipal = new UserPrincipal(userId, UserRole.RESCUE_ORG, true);
            RescueOrganization rescueOrg = mock(RescueOrganization.class);
            when(rescueOrg.getId()).thenReturn(rescueOrgId);
            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));

            DeclinePetRequest request = new DeclinePetRequest("Does not meet our criteria");
            PetDto declinedPet = new PetDto(
                    petId,
                    samplePetDto.name(),
                    samplePetDto.species(),
                    samplePetDto.breed(),
                    samplePetDto.age(),
                    samplePetDto.ageUnit(),
                    samplePetDto.sex(),
                    samplePetDto.size(),
                    samplePetDto.microchipId(),
                    samplePetDto.description(),
                    null,
                    PetStatus.DRAFT,
                    userId,
                    null,
                    Instant.now(),
                    List.of(),
                    null
            );
            when(petService.declineByRescue(petId, rescueOrgId, userId, "Does not meet our criteria")).thenReturn(declinedPet);

            ResponseEntity<PetDto> response = petController.declineByRescue(petId, rescuePrincipal, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo(PetStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("POST /api/pets/{id}/withdraw")
    class WithdrawPet {

        @Test
        @DisplayName("should withdraw pet")
        void shouldWithdrawPet() {
            PetDto withdrawnPet = new PetDto(
                    petId,
                    samplePetDto.name(),
                    samplePetDto.species(),
                    samplePetDto.breed(),
                    samplePetDto.age(),
                    samplePetDto.ageUnit(),
                    samplePetDto.sex(),
                    samplePetDto.size(),
                    samplePetDto.microchipId(),
                    samplePetDto.description(),
                    null,
                    PetStatus.DRAFT,
                    userId,
                    null,
                    Instant.now(),
                    List.of(),
                    null
            );
            when(petService.withdrawPet(petId, userId)).thenReturn(withdrawnPet);

            ResponseEntity<PetDto> response = petController.withdrawPet(petId, fosterPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo(PetStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("GET /api/pets/my")
    class GetMyPets {

        @Test
        @DisplayName("should return pets for foster")
        void shouldReturnPetsForFoster() {
            List<PetDto> pets = List.of(samplePetDto);
            when(petService.getPetsByFoster(userId)).thenReturn(pets);

            ResponseEntity<List<PetDto>> response = petController.getMyPets(fosterPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("GET /api/pets/lookup")
    class LookupByMicrochip {

        @Test
        @DisplayName("should lookup pet by microchip")
        void shouldLookupPetByMicrochip() {
            UserPrincipal vetPrincipal = new UserPrincipal(userId, UserRole.VET, true);
            when(petService.findByMicrochipForVet("MC123456789", userId)).thenReturn(samplePetDto);

            ResponseEntity<PetDto> response = petController.lookupByMicrochip("MC123456789", vetPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().microchipId()).isEqualTo("MC123456789");
        }
    }
}
