package com.example.foreverhome.service;

import com.example.foreverhome.controller.VetController.VetSignOffRequest;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetImage;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.pet.PetStatusHistory;
import com.example.foreverhome.domain.profile.Foster;
import com.example.foreverhome.dto.pet.CreatePetRequest;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.dto.pet.UpdatePetRequest;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.DuplicateMicrochipException;
import com.example.foreverhome.exception.InvalidStatusTransitionException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.FosterRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.PetStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PetService {

    private final PetRepository petRepository;
    private final PetImageRepository petImageRepository;
    private final FosterRepository fosterRepository;
    private final NotificationService notificationService;
    private final VetApprovalService vetApprovalService;
    private final PetStatusHistoryRepository statusHistoryRepository;
    private final MetricsService metricsService;
    private final S3StorageService storageService;

    public PetService(PetRepository petRepository,
                      PetImageRepository petImageRepository,
                      FosterRepository fosterRepository,
                      NotificationService notificationService,
                      VetApprovalService vetApprovalService,
                      PetStatusHistoryRepository statusHistoryRepository,
                      MetricsService metricsService,
                      S3StorageService storageService) {
        this.petRepository = petRepository;
        this.petImageRepository = petImageRepository;
        this.fosterRepository = fosterRepository;
        this.notificationService = notificationService;
        this.vetApprovalService = vetApprovalService;
        this.statusHistoryRepository = statusHistoryRepository;
        this.metricsService = metricsService;
        this.storageService = storageService;
    }

    private void recordStatusChange(UUID petId, PetStatus fromStatus, PetStatus toStatus, UUID changedBy, String notes) {
        PetStatusHistory history = PetStatusHistory.create(petId, fromStatus, toStatus, changedBy, notes);
        statusHistoryRepository.save(history);

        // Record metric for status transition
        metricsService.recordPetStatusChange(
                fromStatus != null ? fromStatus.name() : "NONE",
                toStatus.name()
        );
    }

    private PetDto toPetDtoWithImages(Pet pet) {
        List<String> imageUrls = petImageRepository.findByPetIdOrderByDisplayOrder(pet.getId())
                .stream()
                .map(img -> storageService.getPublicUrl(img.getS3Key()))
                .toList();
        return PetDto.from(pet, imageUrls);
    }

    public PetDto createPet(UUID userId, CreatePetRequest request) {
        // Look up the foster profile by user ID
        Foster foster = fosterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foster profile not found for user"));

        // Check for duplicate microchip
        if (petRepository.existsByMicrochipId(request.microchipId())) {
            throw new DuplicateMicrochipException(request.microchipId());
        }

        Pet pet = Pet.create(
                foster.getId(),
                request.name(),
                request.species(),
                request.breed(),
                request.age(),
                request.ageUnit(),
                request.sex(),
                request.size(),
                request.microchipId(),
                request.description(),
                request.healthNotes()
        );
        Pet savedPet = petRepository.save(pet);

        // Record metric
        metricsService.recordPetRegistration(request.species().name());

        return PetDto.from(savedPet);
    }

    @Transactional(readOnly = true)
    public PetDto getPet(UUID petId) {
        Pet pet = findPetOrThrow(petId);
        return toPetDtoWithImages(pet);
    }

    @Transactional(readOnly = true)
    public List<PetDto> getAvailablePets() {
        return petRepository.findByStatus(PetStatus.AVAILABLE).stream()
                .map(this::toPetDtoWithImages)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetDto> getAvailablePetsWithFilters(String species, String size, String sex,
                                                     Integer minAge, Integer maxAge) {
        return petRepository.findAvailableWithFilters(species, size, sex, minAge, maxAge).stream()
                .map(this::toPetDtoWithImages)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetDto> getFeaturedPets() {
        // Return up to 6 most recently available pets for the homepage
        return petRepository.findFeaturedPets(6).stream()
                .map(this::toPetDtoWithImages)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetDto> getPetsByFoster(UUID userId) {
        // Look up foster profile by user ID to get foster's actual ID
        Foster foster = fosterRepository.findByUserId(userId)
                .orElse(null);

        if (foster == null) {
            return List.of();
        }

        return petRepository.findByFosterId(foster.getId()).stream()
                .map(this::toPetDtoWithImages)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetDto> getPetsByRescueOrg(UUID rescueOrgId) {
        return petRepository.findByRescueOrgId(rescueOrgId).stream()
                .map(this::toPetDtoWithImages)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetDto> getPendingPetsForRescueOrg(UUID rescueOrgId) {
        return petRepository.findPendingByRescueOrgId(rescueOrgId).stream()
                .map(this::toPetDtoWithImages)
                .toList();
    }

    /**
     * Get all PENDING_VET pets from rescue organizations that have approved this vet.
     * This provides the vet's work queue without requiring microchip lookup.
     */
    @Transactional(readOnly = true)
    public List<PetDto> getPendingVetPetsForVet(UUID vetUserId) {
        // Get all rescue orgs that have approved this vet
        List<UUID> approvedRescueOrgIds = vetApprovalService.getRescueOrgsForVet(vetUserId)
                .stream()
                .map(org -> org.getId())
                .toList();

        if (approvedRescueOrgIds.isEmpty()) {
            return List.of();
        }

        // Find all PENDING_VET pets from those rescue orgs
        return petRepository.findByStatusAndRescueOrgIdIn(PetStatus.PENDING_VET, approvedRescueOrgIds)
                .stream()
                .map(pet -> {
                    List<String> imageUrls = petImageRepository.findByPetIdOrderByDisplayOrder(pet.getId())
                            .stream()
                            .map(img -> storageService.getPublicUrl(img.getS3Key()))
                            .toList();
                    return PetDto.fromForVet(pet, imageUrls, true); // All pending pets can be signed off
                })
                .toList();
    }

    public PetDto updatePet(UUID petId, UUID userId, UpdatePetRequest request) {
        Pet pet = findPetOrThrow(petId);

        // Look up the foster profile by user ID
        Foster foster = fosterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foster profile not found for user"));
        verifyOwnership(pet, foster.getId());

        if (!pet.getStatus().isEditable()) {
            throw new InvalidStatusTransitionException("Pet cannot be edited in status: " + pet.getStatus());
        }

        if (request.name() != null) pet.updateName(request.name());
        if (request.description() != null) pet.updateDescription(request.description());
        if (request.healthNotes() != null) pet.updateHealthNotes(request.healthNotes());

        Pet savedPet = petRepository.save(pet);
        return PetDto.from(savedPet);
    }

    public PetDto submitForReview(UUID petId, UUID userId, UUID rescueOrgId) {
        Pet pet = findPetOrThrow(petId);

        // Look up the foster profile by user ID
        Foster foster = fosterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foster profile not found for user"));
        verifyOwnership(pet, foster.getId());

        if (!pet.canTransitionTo(PetStatus.PENDING_RESCUE)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.PENDING_RESCUE.name()
            );
        }

        PetStatus fromStatus = pet.getStatus();
        pet.submitForReview(rescueOrgId);
        Pet savedPet = petRepository.save(pet);

        recordStatusChange(petId, fromStatus, PetStatus.PENDING_RESCUE, userId, "Submitted for rescue review");

        notificationService.notifyRescueOrgPetSubmitted(rescueOrgId, pet);

        return PetDto.from(savedPet);
    }

    public PetDto acceptByRescue(UUID petId, UUID rescueOrgId, UUID rescueUserId) {
        Pet pet = findPetOrThrow(petId);
        verifyRescueOrgOwnership(pet, rescueOrgId);

        if (!pet.canTransitionTo(PetStatus.PENDING_VET)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.PENDING_VET.name()
            );
        }

        PetStatus fromStatus = pet.getStatus();
        pet.acceptByRescue();
        Pet savedPet = petRepository.save(pet);

        recordStatusChange(petId, fromStatus, PetStatus.PENDING_VET, rescueUserId, "Accepted by rescue organization");

        notificationService.notifyFosterPetAccepted(pet.getFosterId(), pet);

        return PetDto.from(savedPet);
    }

    public PetDto declineByRescue(UUID petId, UUID rescueOrgId, UUID rescueUserId, String reason) {
        Pet pet = findPetOrThrow(petId);
        verifyRescueOrgOwnership(pet, rescueOrgId);

        if (!pet.canTransitionTo(PetStatus.DRAFT)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.DRAFT.name()
            );
        }

        PetStatus fromStatus = pet.getStatus();
        pet.declineByRescue();
        Pet savedPet = petRepository.save(pet);

        recordStatusChange(petId, fromStatus, PetStatus.DRAFT, rescueUserId, "Declined by rescue organization: " + reason);

        notificationService.notifyFosterPetDeclined(pet.getFosterId(), pet, reason);

        return PetDto.from(savedPet);
    }

    public PetDto signOffByVet(UUID petId, UUID vetUserId) {
        Pet pet = findPetOrThrow(petId);

        if (!pet.canTransitionTo(PetStatus.AVAILABLE)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.AVAILABLE.name()
            );
        }

        PetStatus fromStatus = pet.getStatus();
        pet.signOffByVet();
        Pet savedPet = petRepository.save(pet);

        recordStatusChange(petId, fromStatus, PetStatus.AVAILABLE, vetUserId, "Signed off by vet");

        // Record vet sign-off metric
        metricsService.recordVetSignOff(true);

        notificationService.notifyFosterPetAvailable(pet.getFosterId(), pet);

        return PetDto.from(savedPet);
    }

    public PetDto declineByVet(UUID petId, UUID vetUserId, String reason) {
        Pet pet = findPetOrThrow(petId);

        if (!pet.canTransitionTo(PetStatus.PENDING_RESCUE)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.PENDING_RESCUE.name()
            );
        }

        PetStatus fromStatus = pet.getStatus();
        pet.declineByVet();
        Pet savedPet = petRepository.save(pet);

        recordStatusChange(petId, fromStatus, PetStatus.PENDING_RESCUE, vetUserId, "Declined by vet: " + reason);

        // Record vet decline metric
        metricsService.recordVetSignOff(false);

        notificationService.notifyFosterPetDeclined(pet.getFosterId(), pet, reason);

        return PetDto.from(savedPet);
    }

    public PetDto withdrawPet(UUID petId, UUID userId) {
        Pet pet = findPetOrThrow(petId);

        // Look up the foster profile by user ID
        Foster foster = fosterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foster profile not found for user"));
        verifyOwnership(pet, foster.getId());

        if (pet.getStatus().isTerminal()) {
            throw new InvalidStatusTransitionException("Cannot withdraw pet in terminal status: " + pet.getStatus());
        }

        PetStatus fromStatus = pet.getStatus();
        pet.withdraw();
        Pet savedPet = petRepository.save(pet);

        recordStatusChange(petId, fromStatus, PetStatus.WITHDRAWN, userId, "Withdrawn by foster");

        return PetDto.from(savedPet);
    }

    @Transactional(readOnly = true)
    public PetDto findByMicrochip(String microchipId) {
        Pet pet = petRepository.findByMicrochipId(microchipId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "microchip: " + microchipId));
        return PetDto.from(pet);
    }

    /**
     * Find pet by microchip for a vet, verifying the vet is approved by the pet's rescue organization.
     * Returns pet info with a canSignOff flag indicating if the vet can perform sign-off.
     *
     * @param microchipId The microchip ID to search for
     * @param vetUserId   The user ID of the vet performing the lookup
     * @return The pet DTO if found and vet is approved, with canSignOff flag
     * @throws ResourceNotFoundException if pet not found
     * @throws AccessDeniedException     if vet is not approved by the pet's rescue org
     */
    @Transactional(readOnly = true)
    public PetDto findByMicrochipForVet(String microchipId, UUID vetUserId) {
        Pet pet = petRepository.findByMicrochipId(microchipId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "microchip: " + microchipId));

        // Only check approval for pets that have a rescue org assigned
        if (pet.getRescueOrgId() != null) {
            boolean isApproved = vetApprovalService.isVetUserApprovedByRescueOrg(vetUserId, pet.getRescueOrgId());
            if (!isApproved) {
                throw new AccessDeniedException(
                        "You are not approved to verify pets for this rescue organization. " +
                        "Please contact the rescue organization to request approval.");
            }
        }

        // Return pet info with canSignOff flag - vet can view pet info regardless of status
        // but can only sign off when status is PENDING_VET
        boolean canSignOff = pet.getStatus() == PetStatus.PENDING_VET;
        List<String> imageUrls = petImageRepository.findByPetIdOrderByDisplayOrder(pet.getId())
                .stream()
                .map(img -> storageService.getPublicUrl(img.getS3Key()))
                .toList();
        return PetDto.fromForVet(pet, imageUrls, canSignOff);
    }

    private Pet findPetOrThrow(UUID petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", petId));
    }

    private void verifyOwnership(Pet pet, UUID fosterId) {
        if (!pet.getFosterId().equals(fosterId)) {
            throw new AccessDeniedException("You do not have permission to modify this pet");
        }
    }

    private void verifyRescueOrgOwnership(Pet pet, UUID rescueOrgId) {
        if (pet.getRescueOrgId() == null || !pet.getRescueOrgId().equals(rescueOrgId)) {
            throw new AccessDeniedException("This pet is not associated with your rescue organization");
        }
    }

    /**
     * Sign off on a pet with full validation that the vet is approved by the pet's rescue org.
     */
    public PetDto signOffByVetWithValidation(UUID petId, UUID vetUserId, VetSignOffRequest request) {
        Pet pet = findPetOrThrow(petId);

        // Verify the vet is approved by this pet's rescue org
        if (pet.getRescueOrgId() != null) {
            boolean isApproved = vetApprovalService.isVetUserApprovedByRescueOrg(vetUserId, pet.getRescueOrgId());
            if (!isApproved) {
                throw new AccessDeniedException(
                        "You are not approved to verify pets for this rescue organization. " +
                        "Please contact the rescue organization to request approval.");
            }
        }

        // Validate health requirements
        if (!Boolean.TRUE.equals(request.isNeutered())) {
            throw new InvalidStatusTransitionException("Pet must be neutered before sign-off");
        }
        if (!Boolean.TRUE.equals(request.isVaccinated())) {
            throw new InvalidStatusTransitionException("Pet must be vaccinated before sign-off");
        }
        if (!Boolean.TRUE.equals(request.isHealthy())) {
            throw new InvalidStatusTransitionException("Pet must be healthy before sign-off");
        }

        return signOffByVet(petId, vetUserId);
    }

    /**
     * Decline a pet with full validation that the vet is approved by the pet's rescue org.
     */
    public PetDto declineByVetWithValidation(UUID petId, UUID vetUserId, String reason) {
        Pet pet = findPetOrThrow(petId);

        // Verify the vet is approved by this pet's rescue org
        if (pet.getRescueOrgId() != null) {
            boolean isApproved = vetApprovalService.isVetUserApprovedByRescueOrg(vetUserId, pet.getRescueOrgId());
            if (!isApproved) {
                throw new AccessDeniedException(
                        "You are not approved to verify pets for this rescue organization. " +
                        "Please contact the rescue organization to request approval.");
            }
        }

        return declineByVet(petId, vetUserId, reason);
    }

    /**
     * Get the status history for a pet.
     */
    @Transactional(readOnly = true)
    public List<PetStatusHistory> getStatusHistory(UUID petId) {
        // Verify pet exists
        findPetOrThrow(petId);
        return statusHistoryRepository.findByPetIdOrderByChangedAtDesc(petId);
    }
}
