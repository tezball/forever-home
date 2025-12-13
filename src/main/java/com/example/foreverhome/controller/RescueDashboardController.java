package com.example.foreverhome.controller;

import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetImage;
import com.example.foreverhome.domain.profile.Adopter;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.AdopterRepository;
import com.example.foreverhome.repository.AdoptionApplicationRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for rescue organization dashboard operations.
 * Handles endpoints for managing pets, applications, and rescue-specific data.
 */
@RestController
@RequestMapping("/api/rescues/my")
@PreAuthorize("hasRole('RESCUE_ORG')")
public class RescueDashboardController {

    private final PetRepository petRepository;
    private final PetImageRepository petImageRepository;
    private final AdoptionApplicationRepository applicationRepository;
    private final RescueOrganizationRepository rescueOrganizationRepository;
    private final AdopterRepository adopterRepository;

    public RescueDashboardController(PetRepository petRepository,
                                      PetImageRepository petImageRepository,
                                      AdoptionApplicationRepository applicationRepository,
                                      RescueOrganizationRepository rescueOrganizationRepository,
                                      AdopterRepository adopterRepository) {
        this.petRepository = petRepository;
        this.petImageRepository = petImageRepository;
        this.applicationRepository = applicationRepository;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
        this.adopterRepository = adopterRepository;
    }

    /**
     * Get the rescue organization for the current user.
     */
    private RescueOrganization getRescueOrgForUser(UUID userId) {
        return rescueOrganizationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization profile not found for user"));
    }

    /**
     * Convert a Pet to PetDto with images.
     */
    private PetDto toPetDtoWithImages(Pet pet) {
        List<String> imageUrls = petImageRepository.findByPetIdOrderByDisplayOrder(pet.getId())
                .stream()
                .map(PetImage::getUrl)
                .toList();
        return PetDto.from(pet, imageUrls);
    }

    /**
     * Get pets pending review (status = PENDING_RESCUE).
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PetDto>> getPendingPets(@AuthenticationPrincipal UserPrincipal principal) {
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());
        List<Pet> pendingPets = petRepository.findPendingByRescueOrgId(rescueOrg.getId());
        List<PetDto> petDtos = pendingPets.stream()
                .map(this::toPetDtoWithImages)
                .toList();
        return ResponseEntity.ok(petDtos);
    }

    /**
     * Get all pets for this rescue organization.
     */
    @GetMapping("/pets")
    public ResponseEntity<List<PetDto>> getAllPets(@AuthenticationPrincipal UserPrincipal principal) {
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());
        List<Pet> pets = petRepository.findByRescueOrgId(rescueOrg.getId());
        List<PetDto> petDtos = pets.stream()
                .map(this::toPetDtoWithImages)
                .toList();
        return ResponseEntity.ok(petDtos);
    }

    /**
     * Get all adoption applications for this rescue organization's pets.
     */
    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplications(@AuthenticationPrincipal UserPrincipal principal) {
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());

        // Get all pets for this rescue org
        List<Pet> pets = petRepository.findByRescueOrgId(rescueOrg.getId());
        Map<UUID, Pet> petMap = pets.stream().collect(Collectors.toMap(Pet::getId, p -> p));
        List<UUID> petIds = pets.stream().map(Pet::getId).toList();

        // Get all applications for these pets
        List<AdoptionApplication> allApplications = petIds.stream()
                .flatMap(petId -> applicationRepository.findByPetId(petId).stream())
                .toList();

        // Get all adopters for these applications using batch fetch to avoid N+1 queries
        List<UUID> adopterIds = allApplications.stream().map(AdoptionApplication::getAdopterId).distinct().toList();
        Map<UUID, Adopter> adopterMap = adopterIds.isEmpty()
                ? Map.of()
                : adopterRepository.findAllByIds(adopterIds).stream()
                        .collect(Collectors.toMap(Adopter::getId, a -> a));

        List<ApplicationResponse> responses = allApplications.stream()
                .map(app -> ApplicationResponse.from(app, petMap.get(app.getPetId()), adopterMap.get(app.getAdopterId())))
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Response DTO for adoption applications.
     */
    record ApplicationResponse(
            UUID id,
            UUID petId,
            String petName,
            String petImageUrl,
            UUID adopterId,
            String adopterName,
            String adopterPhone,
            String status,
            String livingSituation,
            String petExperience,
            String whyAdopt,
            Instant submittedAt,
            Instant reviewedAt,
            String rejectionReason
    ) {
        static ApplicationResponse from(AdoptionApplication app, Pet pet, Adopter adopter) {
            return new ApplicationResponse(
                    app.getId(),
                    app.getPetId(),
                    pet != null ? pet.getName() : "Unknown",
                    null, // Image URL - can be fetched separately if needed
                    app.getAdopterId(),
                    adopter != null ? adopter.getFullName() : "Unknown",
                    adopter != null ? adopter.getPhone() : null,
                    app.getStatus().name(),
                    app.getLivingSituation(),
                    app.getPetExperience(),
                    app.getWhyAdopt(),
                    app.getSubmittedAt(),
                    app.getReviewedAt(),
                    app.getRejectionReason()
            );
        }
    }
}
