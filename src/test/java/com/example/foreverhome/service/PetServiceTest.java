package com.example.foreverhome.service;

import com.example.foreverhome.domain.pet.*;
import com.example.foreverhome.domain.profile.Foster;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.pet.CreatePetRequest;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.dto.pet.UpdatePetRequest;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.InvalidStatusTransitionException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.FosterRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PetService")
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetImageRepository petImageRepository;

    @Mock
    private FosterRepository fosterRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VetApprovalService vetApprovalService;

    private PetService petService;

    @BeforeEach
    void setUp() {
        petService = new PetService(petRepository, petImageRepository, fosterRepository, notificationService, vetApprovalService);
    }

    @Nested
    @DisplayName("createPet")
    class CreatePet {

        @Test
        @DisplayName("given valid pet data from foster, when create, then creates pet in draft status")
        void givenValidPetDataFromFoster_whenCreate_thenCreatesPetInDraftStatus() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            Foster foster = Foster.create(userId, "John", "Doe", "555-1234", null);
            // Use reflection to set the foster's id since it's set internally
            try {
                java.lang.reflect.Field idField = Foster.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(foster, fosterId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            CreatePetRequest request = new CreatePetRequest(
                    "Buddy",
                    Species.DOG,
                    "Golden Retriever",
                    3,
                    AgeUnit.YEARS,
                    PetSex.MALE,
                    PetSize.LARGE,
                    "CHIP123456",
                    "Friendly and energetic",
                    null
            );
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            PetDto result = petService.createPet(userId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Buddy");
            assertThat(result.status()).isEqualTo(PetStatus.DRAFT);

            ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(petCaptor.capture());
            Pet savedPet = petCaptor.getValue();
            assertThat(savedPet.getFosterId()).isEqualTo(fosterId);
            assertThat(savedPet.getStatus()).isEqualTo(PetStatus.DRAFT);
        }

        @Test
        @DisplayName("given user without foster profile, when create, then throws ResourceNotFoundException")
        void givenUserWithoutFosterProfile_whenCreate_thenThrowsResourceNotFoundException() {
            // Given
            UUID userId = UUID.randomUUID();
            CreatePetRequest request = new CreatePetRequest(
                    "Buddy",
                    Species.DOG,
                    "Golden Retriever",
                    3,
                    AgeUnit.YEARS,
                    PetSex.MALE,
                    PetSize.LARGE,
                    "CHIP123456",
                    "Friendly and energetic",
                    null
            );
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> petService.createPet(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Foster profile not found");
        }
    }

    @Nested
    @DisplayName("getPet")
    class GetPet {

        @Test
        @DisplayName("given existing pet id, when get, then returns pet")
        void givenExistingPetId_whenGet_thenReturnsPet() {
            // Given
            UUID petId = UUID.randomUUID();
            Pet pet = createTestPet();
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When
            PetDto result = petService.getPet(petId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("given non-existent pet id, when get, then throws ResourceNotFoundException")
        void givenNonExistentPetId_whenGet_thenThrowsResourceNotFoundException() {
            // Given
            UUID petId = UUID.randomUUID();
            when(petRepository.findById(petId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> petService.getPet(petId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Pet");
        }
    }

    @Nested
    @DisplayName("getAvailablePets")
    class GetAvailablePets {

        @Test
        @DisplayName("when get available, then returns only available status pets")
        void whenGetAvailable_thenReturnsOnlyAvailableStatusPets() {
            // Given
            Pet pet1 = createTestPet();
            pet1.submitForReview(UUID.randomUUID());
            pet1.acceptByRescue();
            pet1.signOffByVet();

            when(petRepository.findByStatus(PetStatus.AVAILABLE)).thenReturn(List.of(pet1));

            // When
            List<PetDto> result = petService.getAvailablePets();

            // Then
            assertThat(result).hasSize(1);
            verify(petRepository).findByStatus(PetStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("updatePet")
    class UpdatePet {

        @Test
        @DisplayName("given pet in draft status and owner is foster, when update, then updates pet")
        void givenPetInDraftStatusAndOwnerIsFoster_whenUpdate_thenUpdatesPet() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            Pet pet = Pet.create(
                    fosterId, "Buddy", Species.DOG, "Golden Retriever",
                    3, AgeUnit.YEARS, PetSex.MALE, PetSize.LARGE,
                    "CHIP123456", "Description", null
            );
            UpdatePetRequest request = new UpdatePetRequest(
                    "Max",
                    null, null, null, null, null, null, null,
                    "Updated description", null
            );
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            PetDto result = petService.updatePet(petId, fosterId, request);

            // Then
            assertThat(result.name()).isEqualTo("Max");
            assertThat(result.description()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("given pet not owned by foster, when update, then throws AccessDeniedException")
        void givenPetNotOwnedByFoster_whenUpdate_thenThrowsAccessDeniedException() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            UUID differentFosterId = UUID.randomUUID();
            Pet pet = Pet.create(
                    differentFosterId, "Buddy", Species.DOG, "Golden Retriever",
                    3, AgeUnit.YEARS, PetSex.MALE, PetSize.LARGE,
                    "CHIP123456", "Description", null
            );
            UpdatePetRequest request = new UpdatePetRequest(
                    "Max", null, null, null, null, null, null, null, null, null
            );
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When/Then
            assertThatThrownBy(() -> petService.updatePet(petId, fosterId, request))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("submitForReview")
    class SubmitForReview {

        @Test
        @DisplayName("given pet in draft status, when submit, then status changes to pending rescue")
        void givenPetInDraftStatus_whenSubmit_thenStatusChangesToPendingRescue() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            Pet pet = Pet.create(
                    fosterId, "Buddy", Species.DOG, "Golden Retriever",
                    3, AgeUnit.YEARS, PetSex.MALE, PetSize.LARGE,
                    "CHIP123456", "Description", null
            );
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            PetDto result = petService.submitForReview(petId, fosterId, rescueOrgId);

            // Then
            assertThat(result.status()).isEqualTo(PetStatus.PENDING_RESCUE);
            verify(notificationService).notifyRescueOrgPetSubmitted(eq(rescueOrgId), any());
        }

        @Test
        @DisplayName("given pet not in draft status, when submit, then throws InvalidStatusTransitionException")
        void givenPetNotInDraftStatus_whenSubmit_thenThrowsInvalidStatusTransitionException() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            Pet pet = Pet.create(
                    fosterId, "Buddy", Species.DOG, "Golden Retriever",
                    3, AgeUnit.YEARS, PetSex.MALE, PetSize.LARGE,
                    "CHIP123456", "Description", null
            );
            pet.submitForReview(rescueOrgId); // Now in PENDING_RESCUE
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When/Then
            assertThatThrownBy(() -> petService.submitForReview(petId, fosterId, rescueOrgId))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("acceptByRescue")
    class AcceptByRescue {

        @Test
        @DisplayName("given pet in pending rescue status, when accept, then status changes to pending vet")
        void givenPetInPendingRescueStatus_whenAccept_thenStatusChangesToPendingVet() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            Pet pet = createTestPet();
            pet.submitForReview(rescueOrgId);
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            PetDto result = petService.acceptByRescue(petId, rescueOrgId);

            // Then
            assertThat(result.status()).isEqualTo(PetStatus.PENDING_VET);
        }
    }

    @Nested
    @DisplayName("declineByRescue")
    class DeclineByRescue {

        @Test
        @DisplayName("given pet in pending rescue status, when decline, then status returns to draft")
        void givenPetInPendingRescueStatus_whenDecline_thenStatusReturnsToDraft() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            Pet pet = createTestPet();
            pet.submitForReview(rescueOrgId);
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            PetDto result = petService.declineByRescue(petId, rescueOrgId, "Not suitable");

            // Then
            assertThat(result.status()).isEqualTo(PetStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("withdrawPet")
    class WithdrawPet {

        @Test
        @DisplayName("given pet in any non-terminal status, when withdraw, then status becomes withdrawn")
        void givenPetInAnyNonTerminalStatus_whenWithdraw_thenStatusBecomesWithdrawn() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            Pet pet = Pet.create(
                    fosterId, "Buddy", Species.DOG, "Golden Retriever",
                    3, AgeUnit.YEARS, PetSex.MALE, PetSize.LARGE,
                    "CHIP123456", "Description", null
            );
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            PetDto result = petService.withdrawPet(petId, fosterId);

            // Then
            assertThat(result.status()).isEqualTo(PetStatus.WITHDRAWN);
        }
    }

    @Nested
    @DisplayName("findByMicrochip")
    class FindByMicrochip {

        @Test
        @DisplayName("given existing microchip and verified vet, when lookup, then returns pet")
        void givenExistingMicrochipAndVerifiedVet_whenLookup_thenReturnsPet() {
            // Given
            String microchip = "CHIP123456";
            Pet pet = createTestPet();
            when(petRepository.findByMicrochipId(microchip)).thenReturn(Optional.of(pet));

            // When
            PetDto result = petService.findByMicrochip(microchip);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.microchipId()).isEqualTo(microchip);
        }

        @Test
        @DisplayName("given non-existent microchip, when lookup, then throws ResourceNotFoundException")
        void givenNonExistentMicrochip_whenLookup_thenThrowsResourceNotFoundException() {
            // Given
            String microchip = "NONEXISTENT";
            when(petRepository.findByMicrochipId(microchip)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> petService.findByMicrochip(microchip))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private Pet createTestPet() {
        return Pet.create(
                UUID.randomUUID(),
                "Buddy",
                Species.DOG,
                "Golden Retriever",
                3,
                AgeUnit.YEARS,
                PetSex.MALE,
                PetSize.LARGE,
                "CHIP123456",
                "A friendly dog",
                null
        );
    }
}
