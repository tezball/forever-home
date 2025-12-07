package com.example.foreverhome.service;

import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.dto.pet.CreatePetRequest;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.dto.pet.UpdatePetRequest;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.InvalidStatusTransitionException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PetService {

    private final PetRepository petRepository;
    private final PetImageRepository petImageRepository;
    private final NotificationService notificationService;

    public PetService(PetRepository petRepository,
                      PetImageRepository petImageRepository,
                      NotificationService notificationService) {
        this.petRepository = petRepository;
        this.petImageRepository = petImageRepository;
        this.notificationService = notificationService;
    }

    public PetDto createPet(UUID fosterId, CreatePetRequest request) {
        Pet pet = Pet.create(
                fosterId,
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
        return PetDto.from(savedPet);
    }

    @Transactional(readOnly = true)
    public PetDto getPet(UUID petId) {
        Pet pet = findPetOrThrow(petId);
        return PetDto.from(pet);
    }

    @Transactional(readOnly = true)
    public List<PetDto> getAvailablePets() {
        return petRepository.findByStatus(PetStatus.AVAILABLE).stream()
                .map(PetDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetDto> getPetsByFoster(UUID fosterId) {
        return petRepository.findByFosterId(fosterId).stream()
                .map(PetDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetDto> getPetsByRescueOrg(UUID rescueOrgId) {
        return petRepository.findByRescueOrgId(rescueOrgId).stream()
                .map(PetDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetDto> getPendingPetsForRescueOrg(UUID rescueOrgId) {
        return petRepository.findPendingByRescueOrgId(rescueOrgId).stream()
                .map(PetDto::from)
                .toList();
    }

    public PetDto updatePet(UUID petId, UUID fosterId, UpdatePetRequest request) {
        Pet pet = findPetOrThrow(petId);
        verifyOwnership(pet, fosterId);

        if (!pet.getStatus().isEditable()) {
            throw new InvalidStatusTransitionException("Pet cannot be edited in status: " + pet.getStatus());
        }

        if (request.name() != null) pet.updateName(request.name());
        if (request.description() != null) pet.updateDescription(request.description());
        if (request.healthNotes() != null) pet.updateHealthNotes(request.healthNotes());

        Pet savedPet = petRepository.save(pet);
        return PetDto.from(savedPet);
    }

    public PetDto submitForReview(UUID petId, UUID fosterId, UUID rescueOrgId) {
        Pet pet = findPetOrThrow(petId);
        verifyOwnership(pet, fosterId);

        if (!pet.canTransitionTo(PetStatus.PENDING_RESCUE)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.PENDING_RESCUE.name()
            );
        }

        pet.submitForReview(rescueOrgId);
        Pet savedPet = petRepository.save(pet);

        notificationService.notifyRescueOrgPetSubmitted(rescueOrgId, pet);

        return PetDto.from(savedPet);
    }

    public PetDto acceptByRescue(UUID petId, UUID rescueOrgId) {
        Pet pet = findPetOrThrow(petId);
        verifyRescueOrgOwnership(pet, rescueOrgId);

        if (!pet.canTransitionTo(PetStatus.PENDING_VET)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.PENDING_VET.name()
            );
        }

        pet.acceptByRescue();
        Pet savedPet = petRepository.save(pet);

        notificationService.notifyFosterPetAccepted(pet.getFosterId(), pet);

        return PetDto.from(savedPet);
    }

    public PetDto declineByRescue(UUID petId, UUID rescueOrgId, String reason) {
        Pet pet = findPetOrThrow(petId);
        verifyRescueOrgOwnership(pet, rescueOrgId);

        if (!pet.canTransitionTo(PetStatus.DRAFT)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.DRAFT.name()
            );
        }

        pet.declineByRescue();
        Pet savedPet = petRepository.save(pet);

        notificationService.notifyFosterPetDeclined(pet.getFosterId(), pet, reason);

        return PetDto.from(savedPet);
    }

    public PetDto signOffByVet(UUID petId) {
        Pet pet = findPetOrThrow(petId);

        if (!pet.canTransitionTo(PetStatus.AVAILABLE)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.AVAILABLE.name()
            );
        }

        pet.signOffByVet();
        Pet savedPet = petRepository.save(pet);

        notificationService.notifyFosterPetAvailable(pet.getFosterId(), pet);

        return PetDto.from(savedPet);
    }

    public PetDto declineByVet(UUID petId, String reason) {
        Pet pet = findPetOrThrow(petId);

        if (!pet.canTransitionTo(PetStatus.PENDING_RESCUE)) {
            throw new InvalidStatusTransitionException(
                    pet.getStatus().name(), PetStatus.PENDING_RESCUE.name()
            );
        }

        pet.declineByVet();
        Pet savedPet = petRepository.save(pet);

        notificationService.notifyFosterPetDeclined(pet.getFosterId(), pet, reason);

        return PetDto.from(savedPet);
    }

    public PetDto withdrawPet(UUID petId, UUID fosterId) {
        Pet pet = findPetOrThrow(petId);
        verifyOwnership(pet, fosterId);

        if (pet.getStatus().isTerminal()) {
            throw new InvalidStatusTransitionException("Cannot withdraw pet in terminal status: " + pet.getStatus());
        }

        pet.withdraw();
        Pet savedPet = petRepository.save(pet);
        return PetDto.from(savedPet);
    }

    @Transactional(readOnly = true)
    public PetDto findByMicrochip(String microchipId) {
        Pet pet = petRepository.findByMicrochipId(microchipId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "microchip: " + microchipId));
        return PetDto.from(pet);
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
}
