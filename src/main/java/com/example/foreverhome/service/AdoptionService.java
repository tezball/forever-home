package com.example.foreverhome.service;

import com.example.foreverhome.domain.adoption.Adoption;
import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.domain.adoption.ApplicationStatus;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.profile.Adopter;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.InvalidStatusTransitionException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.AdopterRepository;
import com.example.foreverhome.repository.AdoptionApplicationRepository;
import com.example.foreverhome.repository.AdoptionRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.VetSignOffRepository;
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
    private final AdopterRepository adopterRepository;
    private final VetSignOffRepository vetSignOffRepository;
    private final NotificationService notificationService;
    private final MetricsService metricsService;

    public AdoptionService(AdoptionApplicationRepository applicationRepository,
                           AdoptionRepository adoptionRepository,
                           PetRepository petRepository,
                           AdopterRepository adopterRepository,
                           VetSignOffRepository vetSignOffRepository,
                           NotificationService notificationService,
                           MetricsService metricsService) {
        this.applicationRepository = applicationRepository;
        this.adoptionRepository = adoptionRepository;
        this.petRepository = petRepository;
        this.adopterRepository = adopterRepository;
        this.vetSignOffRepository = vetSignOffRepository;
        this.notificationService = notificationService;
        this.metricsService = metricsService;
    }

    /**
     * Submit an adoption application for a pet.
     * @param userId The user ID (not the adopter profile ID)
     * @param petId The pet ID to apply for
     * @param message Optional message from the adopter
     */
    public AdoptionApplication submitApplication(UUID userId, UUID petId, String message) {
        Adopter adopter = getAdopterByUserId(userId);
        UUID adopterId = adopter.getId();

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

        // Record metric
        metricsService.recordApplicationSubmitted();

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
    public List<AdoptionApplication> getApplicationsForAdopter(UUID userId) {
        Adopter adopter = getAdopterByUserId(userId);
        return applicationRepository.findByAdopterId(adopter.getId());
    }

    @Transactional(readOnly = true)
    public boolean hasActiveApplicationForPet(UUID userId, UUID petId) {
        Adopter adopter = getAdopterByUserId(userId);
        return applicationRepository.findByPetIdAndAdopterId(petId, adopter.getId())
                .map(app -> app.getStatus() == ApplicationStatus.SUBMITTED ||
                            app.getStatus() == ApplicationStatus.UNDER_REVIEW ||
                            app.getStatus() == ApplicationStatus.APPROVED)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public int getActiveApplicationCount(UUID userId) {
        Adopter adopter = getAdopterByUserId(userId);
        return applicationRepository.countActiveByAdopterId(adopter.getId());
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

        // Record metric
        metricsService.recordApplicationApproved();

        // Reject all other active applications for this pet
        rejectOtherApplications(application.getPetId(), applicationId);

        // Notify adopter
        notificationService.notifyApplicationStatusChange(
                application.getAdopterId(), pet.getName(), "Approved"
        );

        return application;
    }

    public AdoptionApplication rejectApplication(UUID applicationId, UUID rescueOrgId, UUID reviewerUserId, String reason) {
        AdoptionApplication application = findApplicationOrThrow(applicationId);
        Pet pet = findPetOrThrow(application.getPetId());

        verifyRescueOrgOwnership(pet, rescueOrgId);

        if (!application.canTransitionTo(ApplicationStatus.REJECTED)) {
            throw new InvalidStatusTransitionException(
                    application.getStatus().name(), ApplicationStatus.REJECTED.name()
            );
        }

        application.reject(reviewerUserId, reason);
        AdoptionApplication saved = applicationRepository.save(application);

        // Record metric
        metricsService.recordApplicationRejected();

        // Notify adopter
        notificationService.notifyApplicationStatusChange(
                application.getAdopterId(), pet.getName(), "Rejected"
        );

        return saved;
    }

    public void withdrawApplication(UUID applicationId, UUID userId) {
        Adopter adopter = getAdopterByUserId(userId);
        AdoptionApplication application = findApplicationOrThrow(applicationId);

        if (!application.getAdopterId().equals(adopter.getId())) {
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

        // Get the vet sign-off for this pet to record the vet in the adoption
        UUID vetId = vetSignOffRepository.findByPetId(pet.getId())
                .map(signOff -> signOff.getVetId())
                .orElseThrow(() -> new InvalidStatusTransitionException("Pet must have vet sign-off before finalizing adoption"));

        pet.finalizeAdoption();
        petRepository.save(pet);

        Adoption adoption = Adoption.create(
                application.getPetId(),
                pet.getFosterId(),
                application.getAdopterId(),
                rescueOrgId,
                vetId,
                applicationId
        );
        Adoption saved = adoptionRepository.save(adoption);

        // Record metric
        metricsService.recordAdoptionCompleted();

        // Notify all parties
        notificationService.notifyApplicationStatusChange(
                application.getAdopterId(), pet.getName(), "Finalized - Congratulations!"
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Adoption> getAdoptionsForAdopter(UUID userId) {
        Adopter adopter = getAdopterByUserId(userId);
        return adoptionRepository.findByAdopterId(adopter.getId());
    }

    @Transactional(readOnly = true)
    public List<Adoption> getAdoptionsForRescueOrg(UUID rescueOrgId) {
        return adoptionRepository.findByRescueOrgId(rescueOrgId);
    }

    private void rejectOtherApplications(UUID petId, UUID excludeApplicationId) {
        List<AdoptionApplication> activeApplications = applicationRepository.findActiveByPetId(petId);

        if (activeApplications.isEmpty()) {
            return;
        }

        // Fetch pet once outside the loop to avoid N+1
        Pet pet = findPetOrThrow(petId);

        for (AdoptionApplication app : activeApplications) {
            if (!app.getId().equals(excludeApplicationId)) {
                app.reject();
                applicationRepository.save(app);

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

    private Adopter getAdopterByUserId(UUID userId) {
        return adopterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Adopter profile not found for user"));
    }
}
