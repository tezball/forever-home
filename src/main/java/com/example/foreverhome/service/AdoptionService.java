package com.example.foreverhome.service;

import com.example.foreverhome.domain.adoption.Adoption;
import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.domain.adoption.ApplicationStatus;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.InvalidStatusTransitionException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.AdoptionApplicationRepository;
import com.example.foreverhome.repository.AdoptionRepository;
import com.example.foreverhome.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdoptionService {

    private final AdoptionApplicationRepository applicationRepository;
    private final AdoptionRepository adoptionRepository;
    private final PetRepository petRepository;
    private final NotificationService notificationService;

    public AdoptionService(AdoptionApplicationRepository applicationRepository,
                           AdoptionRepository adoptionRepository,
                           PetRepository petRepository,
                           NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.adoptionRepository = adoptionRepository;
        this.petRepository = petRepository;
        this.notificationService = notificationService;
    }

    public AdoptionApplication submitApplication(UUID adopterId, UUID petId, String message) {
        Pet pet = findPetOrThrow(petId);

        if (pet.getStatus() != PetStatus.AVAILABLE) {
            throw new InvalidStatusTransitionException("Pet is not available for adoption");
        }

        // Check if adopter already has an active application for this pet
        applicationRepository.findByPetIdAndAdopterId(petId, adopterId)
                .ifPresent(existing -> {
                    if (existing.getStatus() == ApplicationStatus.SUBMITTED ||
                        existing.getStatus() == ApplicationStatus.UNDER_REVIEW) {
                        throw new IllegalStateException("You already have an active application for this pet");
                    }
                });

        AdoptionApplication application = AdoptionApplication.create(petId, adopterId, message);
        AdoptionApplication saved = applicationRepository.save(application);

        // Notify rescue org
        if (pet.getRescueOrgId() != null) {
            notificationService.notifyNewApplication(pet.getRescueOrgId(), petId, pet.getName());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AdoptionApplication> getApplicationsForPet(UUID petId) {
        return applicationRepository.findByPetId(petId);
    }

    @Transactional(readOnly = true)
    public List<AdoptionApplication> getApplicationsForAdopter(UUID adopterId) {
        return applicationRepository.findByAdopterId(adopterId);
    }

    @Transactional(readOnly = true)
    public List<AdoptionApplication> getActiveApplicationsForPet(UUID petId) {
        return applicationRepository.findActiveByPetId(petId);
    }

    public AdoptionApplication approveApplication(UUID applicationId, UUID rescueOrgId) {
        AdoptionApplication application = findApplicationOrThrow(applicationId);
        Pet pet = findPetOrThrow(application.getPetId());

        verifyRescueOrgOwnership(pet, rescueOrgId);

        if (!application.canTransitionTo(ApplicationStatus.APPROVED)) {
            throw new InvalidStatusTransitionException(
                    application.getStatus().name(), ApplicationStatus.APPROVED.name()
            );
        }

        application.approve();
        pet.approveApplication();

        applicationRepository.save(application);
        petRepository.save(pet);

        // Reject all other active applications for this pet
        rejectOtherApplications(application.getPetId(), applicationId);

        // Notify adopter
        notificationService.notifyApplicationStatusChange(
                application.getAdopterId(), pet.getName(), "Approved"
        );

        return application;
    }

    public AdoptionApplication rejectApplication(UUID applicationId, UUID rescueOrgId, String reason) {
        AdoptionApplication application = findApplicationOrThrow(applicationId);
        Pet pet = findPetOrThrow(application.getPetId());

        verifyRescueOrgOwnership(pet, rescueOrgId);

        if (!application.canTransitionTo(ApplicationStatus.REJECTED)) {
            throw new InvalidStatusTransitionException(
                    application.getStatus().name(), ApplicationStatus.REJECTED.name()
            );
        }

        application.reject();
        AdoptionApplication saved = applicationRepository.save(application);

        // Notify adopter
        notificationService.notifyApplicationStatusChange(
                application.getAdopterId(), pet.getName(), "Rejected"
        );

        return saved;
    }

    public void withdrawApplication(UUID applicationId, UUID adopterId) {
        AdoptionApplication application = findApplicationOrThrow(applicationId);

        if (!application.getAdopterId().equals(adopterId)) {
            throw new AccessDeniedException("You can only withdraw your own applications");
        }

        if (!application.canTransitionTo(ApplicationStatus.WITHDRAWN)) {
            throw new InvalidStatusTransitionException(
                    application.getStatus().name(), ApplicationStatus.WITHDRAWN.name()
            );
        }

        application.withdraw();
        applicationRepository.save(application);
    }

    public Adoption finalizeAdoption(UUID applicationId, UUID rescueOrgId) {
        AdoptionApplication application = findApplicationOrThrow(applicationId);
        Pet pet = findPetOrThrow(application.getPetId());

        verifyRescueOrgOwnership(pet, rescueOrgId);

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new InvalidStatusTransitionException("Application must be approved before finalizing");
        }

        pet.finalizeAdoption();
        petRepository.save(pet);

        Adoption adoption = Adoption.create(
                application.getPetId(),
                application.getAdopterId(),
                pet.getFosterId(),
                rescueOrgId
        );
        Adoption saved = adoptionRepository.save(adoption);

        // Notify all parties
        notificationService.notifyApplicationStatusChange(
                application.getAdopterId(), pet.getName(), "Finalized - Congratulations!"
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Adoption> getAdoptionsForAdopter(UUID adopterId) {
        return adoptionRepository.findByAdopterId(adopterId);
    }

    @Transactional(readOnly = true)
    public List<Adoption> getAdoptionsForRescueOrg(UUID rescueOrgId) {
        return adoptionRepository.findByRescueOrgId(rescueOrgId);
    }

    private void rejectOtherApplications(UUID petId, UUID excludeApplicationId) {
        List<AdoptionApplication> activeApplications = applicationRepository.findActiveByPetId(petId);
        for (AdoptionApplication app : activeApplications) {
            if (!app.getId().equals(excludeApplicationId)) {
                app.reject();
                applicationRepository.save(app);

                Pet pet = findPetOrThrow(petId);
                notificationService.notifyApplicationStatusChange(
                        app.getAdopterId(), pet.getName(), "Rejected - Another applicant was selected"
                );
            }
        }
    }

    private AdoptionApplication findApplicationOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("AdoptionApplication", applicationId));
    }

    private Pet findPetOrThrow(UUID petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", petId));
    }

    private void verifyRescueOrgOwnership(Pet pet, UUID rescueOrgId) {
        if (pet.getRescueOrgId() == null || !pet.getRescueOrgId().equals(rescueOrgId)) {
            throw new AccessDeniedException("This pet is not associated with your rescue organization");
        }
    }
}
