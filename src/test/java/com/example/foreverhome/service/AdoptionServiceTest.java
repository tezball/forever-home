package com.example.foreverhome.service;

import com.example.foreverhome.domain.adoption.Adoption;
import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.domain.adoption.ApplicationStatus;
import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.Breed;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.pet.Species;
import com.example.foreverhome.domain.verification.VetSignOff;
import com.example.foreverhome.domain.profile.Adopter;
import com.example.foreverhome.domain.profile.Foster;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.InvalidStatusTransitionException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.AdopterRepository;
import com.example.foreverhome.repository.AdoptionApplicationRepository;
import com.example.foreverhome.repository.AdoptionRepository;
import com.example.foreverhome.repository.FosterRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.VetSignOffRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdoptionService")
class AdoptionServiceTest {

    @Mock
    private AdoptionApplicationRepository applicationRepository;

    @Mock
    private AdoptionRepository adoptionRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private FosterRepository fosterRepository;

    @Mock
    private VetSignOffRepository vetSignOffRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MetricsService metricsService;

    @Mock
    private FavoriteService favoriteService;

    private AdoptionService adoptionService;

    @BeforeEach
    void setUp() {
        adoptionService = new AdoptionService(
                applicationRepository,
                adoptionRepository,
                petRepository,
                adopterRepository,
                fosterRepository,
                vetSignOffRepository,
                notificationService,
                metricsService,
                favoriteService
        );
    }

    @Nested
    @DisplayName("submitApplication")
    class SubmitApplication {

        @Test
        @DisplayName("given available pet and valid adopter, when submit, then creates application with submitted status")
        void givenAvailablePetAndValidAdopter_whenSubmit_thenCreatesApplicationWithSubmittedStatus() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            String message = "I love dogs";

            Adopter adopter = createTestAdopter(userId, adopterId);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, fosterId, rescueOrgId);
            Foster foster = createTestFoster(UUID.randomUUID(), fosterId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.findByPetIdAndAdopterId(petId, adopterId)).thenReturn(Optional.empty());
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(foster));

            // When
            AdoptionApplication result = adoptionService.submitApplication(userId, petId, message);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            assertThat(result.getPetId()).isEqualTo(petId);
            assertThat(result.getAdopterId()).isEqualTo(adopterId);

            verify(applicationRepository).save(any(AdoptionApplication.class));
            verify(metricsService).recordApplicationSubmitted();
        }

        @Test
        @DisplayName("given available pet and valid adopter, when submit, then notifies all parties")
        void givenAvailablePetAndValidAdopter_whenSubmit_thenNotifiesAllParties() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            UUID fosterUserId = UUID.randomUUID();

            Adopter adopter = createTestAdopter(userId, adopterId);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, fosterId, rescueOrgId);
            Foster foster = createTestFoster(fosterUserId, fosterId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.findByPetIdAndAdopterId(petId, adopterId)).thenReturn(Optional.empty());
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(foster));

            // When
            adoptionService.submitApplication(userId, petId, "I want to adopt");

            // Then
            verify(notificationService).notifyAdopterApplicationSubmitted(eq(userId), eq("Buddy"));
            verify(notificationService).notifyNewApplication(eq(rescueOrgId), eq(petId), eq("Buddy"));
            verify(notificationService).notifyFosterApplicationReceived(eq(fosterUserId), eq("Buddy"));
        }

        @Test
        @DisplayName("given pet not available, when submit, then throws InvalidStatusTransitionException")
        void givenPetNotAvailable_whenSubmit_thenThrowsInvalidStatusTransitionException() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            Adopter adopter = createTestAdopter(userId, adopterId);
            Pet pet = createTestPet(); // DRAFT status

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When/Then
            assertThatThrownBy(() -> adoptionService.submitApplication(userId, petId, "message"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("not available for adoption");
        }

        @Test
        @DisplayName("given pet in progress, when submit, then throws InvalidStatusTransitionException")
        void givenPetInProgress_whenSubmit_thenThrowsInvalidStatusTransitionException() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            Adopter adopter = createTestAdopter(userId, adopterId);
            Pet pet = createTestPetWithStatus(PetStatus.IN_PROGRESS, UUID.randomUUID(), UUID.randomUUID());

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When/Then
            assertThatThrownBy(() -> adoptionService.submitApplication(userId, petId, "message"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("not available for adoption");
        }

        @Test
        @DisplayName("given active application exists for same pet, when submit duplicate, then throws IllegalStateException")
        void givenActiveApplicationExistsForSamePet_whenSubmitDuplicate_thenThrowsIllegalStateException() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            Adopter adopter = createTestAdopter(userId, adopterId);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, UUID.randomUUID(), UUID.randomUUID());
            AdoptionApplication existingApp = AdoptionApplication.create(petId, adopterId, "Previous application");

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.findByPetIdAndAdopterId(petId, adopterId)).thenReturn(Optional.of(existingApp));

            // When/Then
            assertThatThrownBy(() -> adoptionService.submitApplication(userId, petId, "new message"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already have an active application");
        }

        @Test
        @DisplayName("given rejected application exists, when submit new, then allows new application")
        void givenRejectedApplicationExists_whenSubmitNew_thenAllowsNewApplication() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();

            Adopter adopter = createTestAdopter(userId, adopterId);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, fosterId, rescueOrgId);
            AdoptionApplication rejectedApp = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.REJECTED);
            Foster foster = createTestFoster(UUID.randomUUID(), fosterId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.findByPetIdAndAdopterId(petId, adopterId)).thenReturn(Optional.of(rejectedApp));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(foster));

            // When
            AdoptionApplication result = adoptionService.submitApplication(userId, petId, "Try again");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            verify(applicationRepository).save(any(AdoptionApplication.class));
        }

        @Test
        @DisplayName("given no adopter profile, when submit, then throws ResourceNotFoundException")
        void givenNoAdopterProfile_whenSubmit_thenThrowsResourceNotFoundException() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adoptionService.submitApplication(userId, petId, "message"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Adopter profile not found");
        }

        @Test
        @DisplayName("given non-existent pet, when submit, then throws ResourceNotFoundException")
        void givenNonExistentPet_whenSubmit_thenThrowsResourceNotFoundException() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            Adopter adopter = createTestAdopter(userId, adopterId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adoptionService.submitApplication(userId, petId, "message"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Pet");
        }
    }

    @Nested
    @DisplayName("approveApplication")
    class ApproveApplication {

        @Test
        @DisplayName("given submitted application, when approve, then transitions to approved status")
        void givenSubmittedApplication_whenApprove_thenTransitionsToApprovedStatus() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, fosterId, rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.findActiveByPetId(petId)).thenReturn(List.of());
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(createTestAdopter(UUID.randomUUID(), adopterId)));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(createTestFoster(UUID.randomUUID(), fosterId)));

            // When
            AdoptionApplication result = adoptionService.approveApplication(applicationId, rescueOrgId);

            // Then
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            verify(metricsService).recordApplicationApproved();
        }

        @Test
        @DisplayName("given submitted application, when approve, then pet status changes to in progress")
        void givenSubmittedApplication_whenApprove_thenPetStatusChangesToInProgress() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, fosterId, rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.findActiveByPetId(petId)).thenReturn(List.of());
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(createTestAdopter(UUID.randomUUID(), adopterId)));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(createTestFoster(UUID.randomUUID(), fosterId)));

            // When
            adoptionService.approveApplication(applicationId, rescueOrgId);

            // Then
            ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(petCaptor.capture());
            assertThat(petCaptor.getValue().getStatus()).isEqualTo(PetStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("given multiple active applications, when approve one, then others are auto-rejected")
        void givenMultipleActiveApplications_whenApproveOne_thenOthersAreAutoRejected() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            UUID otherAdopterId1 = UUID.randomUUID();
            UUID otherAdopterId2 = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            AdoptionApplication otherApp1 = createTestApplicationWithStatus(petId, otherAdopterId1, ApplicationStatus.SUBMITTED);
            AdoptionApplication otherApp2 = createTestApplicationWithStatus(petId, otherAdopterId2, ApplicationStatus.UNDER_REVIEW);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, fosterId, rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.findActiveByPetId(petId)).thenReturn(List.of(otherApp1, otherApp2));
            when(adopterRepository.findById(any())).thenReturn(Optional.of(createTestAdopter(UUID.randomUUID(), UUID.randomUUID())));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(createTestFoster(UUID.randomUUID(), fosterId)));

            // When
            adoptionService.approveApplication(applicationId, rescueOrgId);

            // Then - verify other applications were saved (rejected)
            verify(applicationRepository, atLeast(3)).save(any(AdoptionApplication.class)); // approved + 2 rejected
            verify(notificationService, times(2)).notifyApplicationStatusChange(any(), eq("Buddy"), eq("Rejected - Another applicant was selected"));
        }

        @Test
        @DisplayName("given wrong rescue org, when approve, then throws AccessDeniedException")
        void givenWrongRescueOrg_whenApprove_thenThrowsAccessDeniedException() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID wrongRescueOrgId = UUID.randomUUID();
            UUID actualRescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, UUID.randomUUID(), actualRescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When/Then
            assertThatThrownBy(() -> adoptionService.approveApplication(applicationId, wrongRescueOrgId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("not associated with your rescue organization");
        }

        @Test
        @DisplayName("given already approved application, when approve again, then throws InvalidStatusTransitionException")
        void givenAlreadyApprovedApplication_whenApproveAgain_thenThrowsInvalidStatusTransitionException() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.APPROVED);
            Pet pet = createTestPetWithStatus(PetStatus.IN_PROGRESS, UUID.randomUUID(), rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When/Then
            assertThatThrownBy(() -> adoptionService.approveApplication(applicationId, rescueOrgId))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("given non-existent application, when approve, then throws ResourceNotFoundException")
        void givenNonExistentApplication_whenApprove_thenThrowsResourceNotFoundException() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adoptionService.approveApplication(applicationId, rescueOrgId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("AdoptionApplication");
        }
    }

    @Nested
    @DisplayName("rejectApplication")
    class RejectApplication {

        @Test
        @DisplayName("given active application, when reject with reason, then transitions to rejected status")
        void givenActiveApplication_whenRejectWithReason_thenTransitionsToRejectedStatus() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID reviewerUserId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            String reason = "Not suitable match";

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, UUID.randomUUID(), rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(createTestAdopter(UUID.randomUUID(), adopterId)));

            // When
            AdoptionApplication result = adoptionService.rejectApplication(applicationId, rescueOrgId, reviewerUserId, reason);

            // Then
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
            verify(metricsService).recordApplicationRejected();
        }

        @Test
        @DisplayName("given active application, when reject, then notifies adopter")
        void givenActiveApplication_whenReject_thenNotifiesAdopter() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID reviewerUserId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID adopterUserId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, UUID.randomUUID(), rescueOrgId);
            Adopter adopter = createTestAdopter(adopterUserId, adopterId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(adopter));

            // When
            adoptionService.rejectApplication(applicationId, rescueOrgId, reviewerUserId, "Not suitable");

            // Then
            verify(notificationService).notifyApplicationStatusChange(eq(adopterUserId), eq("Buddy"), eq("Rejected"));
        }

        @Test
        @DisplayName("given wrong rescue org, when reject, then throws AccessDeniedException")
        void givenWrongRescueOrg_whenReject_thenThrowsAccessDeniedException() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID wrongRescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, UUID.randomUUID(), UUID.randomUUID());

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When/Then
            assertThatThrownBy(() -> adoptionService.rejectApplication(applicationId, wrongRescueOrgId, UUID.randomUUID(), "reason"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("given already rejected application, when reject again, then throws InvalidStatusTransitionException")
        void givenAlreadyRejectedApplication_whenRejectAgain_thenThrowsInvalidStatusTransitionException() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.REJECTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, UUID.randomUUID(), rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When/Then
            assertThatThrownBy(() -> adoptionService.rejectApplication(applicationId, rescueOrgId, UUID.randomUUID(), "reason"))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("withdrawApplication")
    class WithdrawApplication {

        @Test
        @DisplayName("given adopter owns application, when withdraw, then transitions to withdrawn status")
        void givenAdopterOwnsApplication_whenWithdraw_thenTransitionsToWithdrawnStatus() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            Adopter adopter = createTestAdopter(userId, adopterId);
            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            adoptionService.withdrawApplication(applicationId, userId);

            // Then
            ArgumentCaptor<AdoptionApplication> captor = ArgumentCaptor.forClass(AdoptionApplication.class);
            verify(applicationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("given different adopter, when withdraw, then throws AccessDeniedException")
        void givenDifferentAdopter_whenWithdraw_thenThrowsAccessDeniedException() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID differentAdopterId = UUID.randomUUID();

            Adopter adopter = createTestAdopter(userId, adopterId);
            AdoptionApplication application = createTestApplicationWithStatus(petId, differentAdopterId, ApplicationStatus.SUBMITTED);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            // When/Then
            assertThatThrownBy(() -> adoptionService.withdrawApplication(applicationId, userId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("only withdraw your own applications");
        }

        @Test
        @DisplayName("given approved application, when withdraw, then throws InvalidStatusTransitionException")
        void givenApprovedApplication_whenWithdraw_thenThrowsInvalidStatusTransitionException() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            Adopter adopter = createTestAdopter(userId, adopterId);
            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.APPROVED);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            // When/Then
            assertThatThrownBy(() -> adoptionService.withdrawApplication(applicationId, userId))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("finalizeAdoption")
    class FinalizeAdoption {

        @Test
        @DisplayName("given approved application with vet sign-off, when finalize, then creates adoption record")
        void givenApprovedApplicationWithVetSignOff_whenFinalize_thenCreatesAdoptionRecord() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            UUID vetId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.APPROVED);
            Pet pet = createTestPetWithStatus(PetStatus.IN_PROGRESS, fosterId, rescueOrgId);
            VetSignOff vetSignOff = mock(VetSignOff.class);
            when(vetSignOff.getVetId()).thenReturn(vetId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(vetSignOffRepository.findByPetId(any())).thenReturn(Optional.of(vetSignOff));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adoptionRepository.save(any(Adoption.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(createTestAdopter(UUID.randomUUID(), adopterId)));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(createTestFoster(UUID.randomUUID(), fosterId)));

            // When
            Adoption result = adoptionService.finalizeAdoption(applicationId, rescueOrgId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPetId()).isEqualTo(petId);
            assertThat(result.getAdopterId()).isEqualTo(adopterId);
            assertThat(result.getRescueOrgId()).isEqualTo(rescueOrgId);
            assertThat(result.getVetId()).isEqualTo(vetId);

            verify(adoptionRepository).save(any(Adoption.class));
            verify(metricsService).recordAdoptionCompleted();
        }

        @Test
        @DisplayName("given approved application with vet sign-off, when finalize, then pet status becomes adopted")
        void givenApprovedApplicationWithVetSignOff_whenFinalize_thenPetStatusBecomesAdopted() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.APPROVED);
            Pet pet = createTestPetWithStatus(PetStatus.IN_PROGRESS, fosterId, rescueOrgId);
            VetSignOff vetSignOff = mock(VetSignOff.class);
            when(vetSignOff.getVetId()).thenReturn(UUID.randomUUID());

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(vetSignOffRepository.findByPetId(any())).thenReturn(Optional.of(vetSignOff));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adoptionRepository.save(any(Adoption.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(createTestAdopter(UUID.randomUUID(), adopterId)));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(createTestFoster(UUID.randomUUID(), fosterId)));

            // When
            adoptionService.finalizeAdoption(applicationId, rescueOrgId);

            // Then
            ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(petCaptor.capture());
            assertThat(petCaptor.getValue().getStatus()).isEqualTo(PetStatus.ADOPTED);
        }

        @Test
        @DisplayName("given approved application with vet sign-off, when finalize, then application status becomes finalized")
        void givenApprovedApplicationWithVetSignOff_whenFinalize_thenApplicationStatusBecomesFinalized() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.APPROVED);
            Pet pet = createTestPetWithStatus(PetStatus.IN_PROGRESS, fosterId, rescueOrgId);
            VetSignOff vetSignOff = mock(VetSignOff.class);
            when(vetSignOff.getVetId()).thenReturn(UUID.randomUUID());

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(vetSignOffRepository.findByPetId(any())).thenReturn(Optional.of(vetSignOff));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adoptionRepository.save(any(Adoption.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(createTestAdopter(UUID.randomUUID(), adopterId)));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(createTestFoster(UUID.randomUUID(), fosterId)));

            // When
            adoptionService.finalizeAdoption(applicationId, rescueOrgId);

            // Then
            ArgumentCaptor<AdoptionApplication> appCaptor = ArgumentCaptor.forClass(AdoptionApplication.class);
            verify(applicationRepository).save(appCaptor.capture());
            assertThat(appCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.FINALIZED);
        }

        @Test
        @DisplayName("given no vet sign-off, when finalize, then throws InvalidStatusTransitionException")
        void givenNoVetSignOff_whenFinalize_thenThrowsInvalidStatusTransitionException() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.APPROVED);
            Pet pet = createTestPetWithStatus(PetStatus.IN_PROGRESS, UUID.randomUUID(), rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(vetSignOffRepository.findByPetId(any())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adoptionService.finalizeAdoption(applicationId, rescueOrgId))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("vet sign-off");
        }

        @Test
        @DisplayName("given submitted application, when finalize, then throws InvalidStatusTransitionException")
        void givenSubmittedApplication_whenFinalize_thenThrowsInvalidStatusTransitionException() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, UUID.randomUUID(), rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

            // When/Then
            assertThatThrownBy(() -> adoptionService.finalizeAdoption(applicationId, rescueOrgId))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("must be approved");
        }

        @Test
        @DisplayName("given finalization, when complete, then notifies adopter and foster and favoritors")
        void givenFinalization_whenComplete_thenNotifiesAdopterAndFosterAndFavoritors() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            UUID adopterUserId = UUID.randomUUID();
            UUID fosterUserId = UUID.randomUUID();

            Pet pet = createTestPetWithStatus(PetStatus.IN_PROGRESS, fosterId, rescueOrgId);
            UUID petId = pet.getId(); // Get actual pet ID
            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.APPROVED);
            VetSignOff vetSignOff = mock(VetSignOff.class);
            when(vetSignOff.getVetId()).thenReturn(UUID.randomUUID());

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(vetSignOffRepository.findByPetId(any())).thenReturn(Optional.of(vetSignOff));
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adoptionRepository.save(any(Adoption.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(createTestAdopter(adopterUserId, adopterId)));
            when(fosterRepository.findById(fosterId)).thenReturn(Optional.of(createTestFoster(fosterUserId, fosterId)));

            // When
            adoptionService.finalizeAdoption(applicationId, rescueOrgId);

            // Then
            verify(notificationService).notifyApplicationStatusChange(eq(adopterUserId), eq("Buddy"), eq("Finalized - Congratulations!"));
            verify(notificationService).notifyFosterAdoptionComplete(eq(fosterUserId), eq("Buddy"));
            verify(favoriteService).notifyFavoritorsOfAdoption(eq(petId), eq("Buddy"));
        }
    }

    @Nested
    @DisplayName("startReview")
    class StartReview {

        @Test
        @DisplayName("given submitted application, when start review, then transitions to under review")
        void givenSubmittedApplication_whenStartReview_thenTransitionsToUnderReview() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID reviewerUserId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, UUID.randomUUID(), rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(createTestAdopter(UUID.randomUUID(), adopterId)));

            // When
            AdoptionApplication result = adoptionService.startReview(applicationId, rescueOrgId, reviewerUserId);

            // Then
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        }

        @Test
        @DisplayName("given submitted application, when start review, then notifies adopter")
        void givenSubmittedApplication_whenStartReview_thenNotifiesAdopter() {
            // Given
            UUID applicationId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            UUID reviewerUserId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            UUID adopterUserId = UUID.randomUUID();

            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);
            Pet pet = createTestPetWithStatus(PetStatus.AVAILABLE, UUID.randomUUID(), rescueOrgId);

            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(applicationRepository.save(any(AdoptionApplication.class))).thenAnswer(inv -> inv.getArgument(0));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(createTestAdopter(adopterUserId, adopterId)));

            // When
            adoptionService.startReview(applicationId, rescueOrgId, reviewerUserId);

            // Then
            verify(notificationService).notifyAdopterApplicationUnderReview(eq(adopterUserId), eq("Buddy"));
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("when get applications for pet, then returns all applications")
        void whenGetApplicationsForPet_thenReturnsAllApplications() {
            // Given
            UUID petId = UUID.randomUUID();
            List<AdoptionApplication> applications = List.of(
                    createTestApplicationWithStatus(petId, UUID.randomUUID(), ApplicationStatus.SUBMITTED),
                    createTestApplicationWithStatus(petId, UUID.randomUUID(), ApplicationStatus.REJECTED)
            );
            when(applicationRepository.findByPetId(petId)).thenReturn(applications);

            // When
            List<AdoptionApplication> result = adoptionService.getApplicationsForPet(petId);

            // Then
            assertThat(result).hasSize(2);
            verify(applicationRepository).findByPetId(petId);
        }

        @Test
        @DisplayName("when get applications for adopter, then returns adopter's applications")
        void whenGetApplicationsForAdopter_thenReturnsAdoptersApplications() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            Adopter adopter = createTestAdopter(userId, adopterId);
            List<AdoptionApplication> applications = List.of(
                    createTestApplicationWithStatus(UUID.randomUUID(), adopterId, ApplicationStatus.SUBMITTED)
            );

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(applicationRepository.findByAdopterId(adopterId)).thenReturn(applications);

            // When
            List<AdoptionApplication> result = adoptionService.getApplicationsForAdopter(userId);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("when has active application for pet with submitted status, then returns true")
        void whenHasActiveApplicationForPetWithSubmittedStatus_thenReturnsTrue() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            Adopter adopter = createTestAdopter(userId, adopterId);
            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.SUBMITTED);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(applicationRepository.findByPetIdAndAdopterId(petId, adopterId)).thenReturn(Optional.of(application));

            // When
            boolean result = adoptionService.hasActiveApplicationForPet(userId, petId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("when has active application for pet with approved status, then returns true")
        void whenHasActiveApplicationForPetWithApprovedStatus_thenReturnsTrue() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            Adopter adopter = createTestAdopter(userId, adopterId);
            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.APPROVED);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(applicationRepository.findByPetIdAndAdopterId(petId, adopterId)).thenReturn(Optional.of(application));

            // When
            boolean result = adoptionService.hasActiveApplicationForPet(userId, petId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("when has active application for pet with rejected status, then returns false")
        void whenHasActiveApplicationForPetWithRejectedStatus_thenReturnsFalse() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            Adopter adopter = createTestAdopter(userId, adopterId);
            AdoptionApplication application = createTestApplicationWithStatus(petId, adopterId, ApplicationStatus.REJECTED);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(applicationRepository.findByPetIdAndAdopterId(petId, adopterId)).thenReturn(Optional.of(application));

            // When
            boolean result = adoptionService.hasActiveApplicationForPet(userId, petId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("when no application exists, then has active returns false")
        void whenNoApplicationExists_thenHasActiveReturnsFalse() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            Adopter adopter = createTestAdopter(userId, adopterId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(applicationRepository.findByPetIdAndAdopterId(petId, adopterId)).thenReturn(Optional.empty());

            // When
            boolean result = adoptionService.hasActiveApplicationForPet(userId, petId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("when get active application count, then returns count from repository")
        void whenGetActiveApplicationCount_thenReturnsCountFromRepository() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            Adopter adopter = createTestAdopter(userId, adopterId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(applicationRepository.countActiveByAdopterId(adopterId)).thenReturn(3);

            // When
            int result = adoptionService.getActiveApplicationCount(userId);

            // Then
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("when get active applications for pet, then returns only active statuses")
        void whenGetActiveApplicationsForPet_thenReturnsOnlyActiveStatuses() {
            // Given
            UUID petId = UUID.randomUUID();
            List<AdoptionApplication> activeApps = List.of(
                    createTestApplicationWithStatus(petId, UUID.randomUUID(), ApplicationStatus.SUBMITTED),
                    createTestApplicationWithStatus(petId, UUID.randomUUID(), ApplicationStatus.UNDER_REVIEW)
            );
            when(applicationRepository.findActiveByPetId(petId)).thenReturn(activeApps);

            // When
            List<AdoptionApplication> result = adoptionService.getActiveApplicationsForPet(petId);

            // Then
            assertThat(result).hasSize(2);
            verify(applicationRepository).findActiveByPetId(petId);
        }

        @Test
        @DisplayName("when get adoptions for adopter, then returns adopter's adoptions")
        void whenGetAdoptionsForAdopter_thenReturnsAdoptersAdoptions() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            Adopter adopter = createTestAdopter(userId, adopterId);
            List<Adoption> adoptions = List.of(mock(Adoption.class));

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(adoptionRepository.findByAdopterId(adopterId)).thenReturn(adoptions);

            // When
            List<Adoption> result = adoptionService.getAdoptionsForAdopter(userId);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("when get adoptions for rescue org, then returns rescue org's adoptions")
        void whenGetAdoptionsForRescueOrg_thenReturnsRescueOrgsAdoptions() {
            // Given
            UUID rescueOrgId = UUID.randomUUID();
            List<Adoption> adoptions = List.of(mock(Adoption.class), mock(Adoption.class));

            when(adoptionRepository.findByRescueOrgId(rescueOrgId)).thenReturn(adoptions);

            // When
            List<Adoption> result = adoptionService.getAdoptionsForRescueOrg(rescueOrgId);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    // Helper methods

    private Pet createTestPet() {
        return Pet.create(
                UUID.randomUUID(),
                "Buddy",
                Species.DOG,
                Breed.GOLDEN_RETRIEVER,
                3,
                AgeUnit.YEARS,
                PetSex.MALE,
                PetSize.LARGE,
                "CHIP123456",
                "A friendly dog",
                null
        );
    }

    private Pet createTestPetWithStatus(PetStatus status, UUID fosterId, UUID rescueOrgId) {
        Pet pet = Pet.create(
                fosterId,
                "Buddy",
                Species.DOG,
                Breed.GOLDEN_RETRIEVER,
                3,
                AgeUnit.YEARS,
                PetSex.MALE,
                PetSize.LARGE,
                "CHIP123456",
                "A friendly dog",
                null
        );

        // Use reflection to set status and rescueOrgId since they're normally set through state transitions
        try {
            java.lang.reflect.Field statusField = Pet.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(pet, status);

            java.lang.reflect.Field rescueOrgField = Pet.class.getDeclaredField("rescueOrgId");
            rescueOrgField.setAccessible(true);
            rescueOrgField.set(pet, rescueOrgId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return pet;
    }

    private Adopter createTestAdopter(UUID userId, UUID adopterId) {
        Adopter adopter = Adopter.create(userId, "John", "Doe", "555-1234", null, null, null);
        try {
            java.lang.reflect.Field idField = Adopter.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(adopter, adopterId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return adopter;
    }

    private Foster createTestFoster(UUID userId, UUID fosterId) {
        Foster foster = Foster.create(userId, "Jane", "Smith", "555-5678", null);
        try {
            java.lang.reflect.Field idField = Foster.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(foster, fosterId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return foster;
    }

    private AdoptionApplication createTestApplicationWithStatus(UUID petId, UUID adopterId, ApplicationStatus status) {
        AdoptionApplication application = AdoptionApplication.create(petId, adopterId, "I want to adopt");

        if (status != ApplicationStatus.SUBMITTED) {
            try {
                java.lang.reflect.Field statusField = AdoptionApplication.class.getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(application, status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return application;
    }
}
