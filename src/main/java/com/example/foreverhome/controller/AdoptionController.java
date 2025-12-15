package com.example.foreverhome.controller;

import com.example.foreverhome.domain.adoption.Adoption;
import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetImage;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.dto.adoption.FinalizeAdoptionRequest;
import com.example.foreverhome.dto.adoption.RejectApplicationRequest;
import com.example.foreverhome.dto.adoption.SubmitApplicationRequest;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.AdoptionService;
import com.example.foreverhome.service.S3StorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AdoptionController {

    private final AdoptionService adoptionService;
    private final RescueOrganizationRepository rescueOrganizationRepository;
    private final PetRepository petRepository;
    private final PetImageRepository petImageRepository;
    private final S3StorageService storageService;

    public AdoptionController(AdoptionService adoptionService,
                              RescueOrganizationRepository rescueOrganizationRepository,
                              PetRepository petRepository,
                              PetImageRepository petImageRepository,
                              S3StorageService storageService) {
        this.adoptionService = adoptionService;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
        this.petRepository = petRepository;
        this.petImageRepository = petImageRepository;
        this.storageService = storageService;
    }

    @PostMapping("/applications")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<AdoptionApplication> submitApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitApplicationRequest request) {
        AdoptionApplication application = adoptionService.submitApplication(
                principal.userId(), request.petId(), request.message()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<List<AdopterApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<AdoptionApplication> applications = adoptionService.getApplicationsForAdopter(principal.userId());

        // Get all pet IDs and fetch pets in batch
        List<UUID> petIds = applications.stream().map(AdoptionApplication::getPetId).distinct().toList();
        Map<UUID, Pet> petMap = petIds.isEmpty() ? Map.of() :
                petRepository.findByIdIn(petIds).stream().collect(Collectors.toMap(Pet::getId, p -> p));

        // Get primary image for each pet
        Map<UUID, String> petImageMap = petIds.isEmpty() ? Map.of() :
                petImageRepository.findPrimaryImagesByPetIds(petIds).stream()
                        .collect(Collectors.toMap(PetImage::getPetId, img -> storageService.getPublicUrl(img.getS3Key()), (a, b) -> a));

        List<AdopterApplicationResponse> responses = applications.stream()
                .map(app -> AdopterApplicationResponse.from(app, petMap.get(app.getPetId()), petImageMap.get(app.getPetId())))
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Response DTO for adopter's view of their applications.
     */
    public record AdopterApplicationResponse(
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
        static AdopterApplicationResponse from(AdoptionApplication app, Pet pet, String imageUrl) {
            return new AdopterApplicationResponse(
                    app.getId(),
                    app.getPetId(),
                    pet != null ? pet.getName() : "Unknown",
                    imageUrl,
                    app.getAdopterId(),
                    null, // adopterName not needed for own applications
                    null, // adopterPhone not needed for own applications
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

    @GetMapping("/applications/check/{petId}")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<ApplicationCheckResponse> checkApplicationStatus(
            @PathVariable UUID petId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean hasApplied = adoptionService.hasActiveApplicationForPet(principal.userId(), petId);
        int activeCount = adoptionService.getActiveApplicationCount(principal.userId());
        return ResponseEntity.ok(new ApplicationCheckResponse(hasApplied, activeCount));
    }

    public record ApplicationCheckResponse(boolean hasApplied, int activeApplicationCount) {}

    @GetMapping("/pets/{petId}/applications")
    @PreAuthorize("hasRole('RESCUE_ORG') or hasRole('ADMIN')")
    public ResponseEntity<List<AdoptionApplication>> getApplicationsForPet(@PathVariable UUID petId) {
        return ResponseEntity.ok(adoptionService.getApplicationsForPet(petId));
    }

    @PutMapping("/applications/{id}/start-review")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<AdoptionApplication> startReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());
        AdoptionApplication application = adoptionService.startReview(id, rescueOrg.getId(), principal.userId());
        return ResponseEntity.ok(application);
    }

    @PutMapping("/applications/{id}/approve")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<AdoptionApplication> approveApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());
        AdoptionApplication application = adoptionService.approveApplication(id, rescueOrg.getId());
        return ResponseEntity.ok(application);
    }

    @PutMapping("/applications/{id}/reject")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<AdoptionApplication> rejectApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RejectApplicationRequest request) {
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());
        AdoptionApplication application = adoptionService.rejectApplication(id, rescueOrg.getId(), principal.userId(), request.reason());
        return ResponseEntity.ok(application);
    }

    @DeleteMapping("/applications/{id}")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<Void> withdrawApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        adoptionService.withdrawApplication(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/adoptions")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<Adoption> finalizeAdoption(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FinalizeAdoptionRequest request) {
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());
        Adoption adoption = adoptionService.finalizeAdoption(request.applicationId(), rescueOrg.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(adoption);
    }

    @GetMapping("/adoptions")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<List<Adoption>> getMyAdoptions(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adoptionService.getAdoptionsForAdopter(principal.userId()));
    }

    private RescueOrganization getRescueOrgForUser(UUID userId) {
        return rescueOrganizationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization profile not found for user"));
    }
}
