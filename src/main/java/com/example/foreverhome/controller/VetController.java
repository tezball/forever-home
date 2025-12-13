package com.example.foreverhome.controller;

import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.verification.VetApprovalRequest;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller for vet-specific operations including pet verification sign-off.
 */
@RestController
@RequestMapping("/api/vet")
@PreAuthorize("hasRole('VET')")
public class VetController {

    private final PetService petService;
    private final VetApprovalService vetApprovalService;
    private final VetApprovalRequestService vetApprovalRequestService;
    private final VetRepository vetRepository;
    private final VetSignOffRepository vetSignOffRepository;
    private final PetRepository petRepository;
    private final RescueOrganizationRepository rescueOrganizationRepository;

    public VetController(PetService petService, VetApprovalService vetApprovalService,
                         VetApprovalRequestService vetApprovalRequestService,
                         VetRepository vetRepository, VetSignOffRepository vetSignOffRepository,
                         PetRepository petRepository, RescueOrganizationRepository rescueOrganizationRepository) {
        this.petService = petService;
        this.vetApprovalService = vetApprovalService;
        this.vetApprovalRequestService = vetApprovalRequestService;
        this.vetRepository = vetRepository;
        this.vetSignOffRepository = vetSignOffRepository;
        this.petRepository = petRepository;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
    }

    /**
     * Lookup a pet by microchip number.
     * The vet must be approved by the pet's rescue organization.
     */
    @GetMapping("/pets/lookup")
    public ResponseEntity<PetDto> lookupByMicrochip(
            @RequestParam String microchip,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(petService.findByMicrochipForVet(microchip, principal.userId()));
    }

    /**
     * Sign off on a pet's health verification, making it available for adoption.
     * The vet must be approved by the pet's rescue organization.
     */
    @PostMapping("/pets/{petId}/sign-off")
    public ResponseEntity<PetDto> signOffPet(
            @PathVariable UUID petId,
            @Valid @RequestBody VetSignOffRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify vet is approved for this pet's rescue org (done in service layer)
        PetDto pet = petService.signOffByVetWithValidation(petId, principal.userId(), request);
        return ResponseEntity.ok(pet);
    }

    /**
     * Decline a pet's verification with a reason.
     * The pet will be returned to the rescue organization for further review.
     */
    @PostMapping("/pets/{petId}/decline")
    public ResponseEntity<PetDto> declinePet(
            @PathVariable UUID petId,
            @Valid @RequestBody VetDeclineRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PetDto pet = petService.declineByVetWithValidation(petId, principal.userId(), request.reason());
        return ResponseEntity.ok(pet);
    }

    /**
     * Get all rescue organizations that have approved this vet.
     */
    @GetMapping("/rescue-orgs")
    public ResponseEntity<List<RescueOrgSummary>> getMyRescueOrgs(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RescueOrganization> orgs = vetApprovalService.getRescueOrgsForVet(principal.userId());
        List<RescueOrgSummary> summaries = orgs.stream()
                .map(RescueOrgSummary::from)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    /**
     * Get all approved organizations with approval dates for dashboard display.
     */
    @GetMapping("/approved-organizations")
    public ResponseEntity<List<ApprovedOrgResponse>> getApprovedOrganizations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ApprovedOrgResponse> responses = vetApprovalService.getApprovedOrgsWithDatesForVet(principal.userId());
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all verified rescue organizations that the vet can request approval from.
     * Excludes organizations where the vet is already approved or has a pending request.
     */
    @GetMapping("/rescue-orgs/available")
    public ResponseEntity<List<RescueOrgSummary>> getAvailableRescueOrgs(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RescueOrganization> orgs = vetApprovalRequestService.getAvailableRescueOrgsForVet(principal.userId());
        List<RescueOrgSummary> summaries = orgs.stream()
                .map(RescueOrgSummary::from)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    /**
     * Submit an approval request to a rescue organization.
     */
    @PostMapping("/rescue-orgs/{rescueOrgId}/request")
    public ResponseEntity<ApprovalRequestResponse> requestApproval(
            @PathVariable UUID rescueOrgId,
            @RequestBody(required = false) ApprovalRequestBody requestBody,
            @AuthenticationPrincipal UserPrincipal principal) {
        String message = requestBody != null ? requestBody.message() : null;
        VetApprovalRequest request = vetApprovalRequestService.requestApproval(
                principal.userId(), rescueOrgId, message);
        RescueOrganization org = rescueOrganizationRepository.findById(rescueOrgId).orElse(null);
        return ResponseEntity.ok(ApprovalRequestResponse.from(request, org));
    }

    /**
     * Get all approval requests made by this vet.
     */
    @GetMapping("/approval-requests")
    public ResponseEntity<List<ApprovalRequestResponse>> getMyApprovalRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<VetApprovalRequest> requests = vetApprovalRequestService.getRequestsForVet(principal.userId());
        List<ApprovalRequestResponse> responses = requests.stream()
                .map(request -> {
                    RescueOrganization org = rescueOrganizationRepository.findById(request.getRescueOrgId()).orElse(null);
                    return ApprovalRequestResponse.from(request, org);
                })
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Withdraw a pending approval request.
     */
    @DeleteMapping("/approval-requests/{requestId}")
    public ResponseEntity<Void> withdrawApprovalRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        vetApprovalRequestService.withdrawRequest(principal.userId(), requestId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get the sign-off history for this vet.
     * Returns all pets that this vet has signed off on.
     */
    @GetMapping("/sign-offs")
    public ResponseEntity<List<SignOffHistoryItem>> getSignOffHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        // Get the vet profile for this user
        Vet vet = vetRepository.findByUserId(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile", "userId: " + principal.userId()));

        // Get all sign-offs by this vet
        List<VetSignOff> signOffs = vetSignOffRepository.findByVetIdOrderBySignedOffAtDesc(vet.getId());

        // Enrich with pet details
        List<SignOffHistoryItem> history = signOffs.stream()
                .map(signOff -> {
                    Pet pet = petRepository.findById(signOff.getPetId()).orElse(null);
                    return SignOffHistoryItem.from(signOff, pet);
                })
                .toList();

        return ResponseEntity.ok(history);
    }

    /**
     * Get the count of sign-offs for this vet.
     */
    @GetMapping("/sign-offs/count")
    public ResponseEntity<SignOffCountResponse> getSignOffCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        Vet vet = vetRepository.findByUserId(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile", "userId: " + principal.userId()));

        long count = vetSignOffRepository.countByVetId(vet.getId());
        return ResponseEntity.ok(new SignOffCountResponse(count));
    }

    // Request DTOs
    public record VetSignOffRequest(
            @NotNull(message = "Neutered confirmation is required")
            Boolean isNeutered,

            @NotNull(message = "Vaccination confirmation is required")
            Boolean isVaccinated,

            @NotNull(message = "Health status is required")
            Boolean isHealthy,

            String healthNotes
    ) {}

    public record VetDeclineRequest(
            @NotBlank(message = "Reason is required when declining")
            String reason
    ) {}

    public record ApprovalRequestBody(
            String message
    ) {}

    // Response DTOs
    record RescueOrgSummary(
            UUID id,
            String name,
            String city,
            String state,
            String phone
    ) {
        static RescueOrgSummary from(RescueOrganization org) {
            String city = org.getAddress() != null ? org.getAddress().city() : null;
            String state = org.getAddress() != null ? org.getAddress().state() : null;
            return new RescueOrgSummary(
                    org.getId(),
                    org.getName(),
                    city,
                    state,
                    org.getPhone()
            );
        }
    }

    record SignOffHistoryItem(
            UUID id,
            UUID petId,
            String petName,
            String petSpecies,
            String petBreed,
            String petMicrochipId,
            String petImageUrl,
            String healthStatus,
            String healthNotes,
            Instant signedOffAt
    ) {
        static SignOffHistoryItem from(VetSignOff signOff, Pet pet) {
            return new SignOffHistoryItem(
                    signOff.getId(),
                    signOff.getPetId(),
                    pet != null ? pet.getName() : "Unknown",
                    pet != null ? pet.getSpecies().name() : null,
                    pet != null ? pet.getBreed() : null,
                    pet != null ? pet.getMicrochipId() : null,
                    null, // Pet image URL - would need to fetch from images table
                    signOff.getHealthStatus().name(),
                    signOff.getHealthNotes(),
                    signOff.getSignedOffAt()
            );
        }
    }

    record SignOffCountResponse(long count) {}

    record ApprovalRequestResponse(
            UUID id,
            UUID rescueOrgId,
            String rescueOrgName,
            String status,
            String message,
            Instant requestedAt,
            Instant reviewedAt,
            String rejectionReason
    ) {
        static ApprovalRequestResponse from(VetApprovalRequest request, RescueOrganization org) {
            return new ApprovalRequestResponse(
                    request.getId(),
                    request.getRescueOrgId(),
                    org != null ? org.getName() : "Unknown",
                    request.getStatus().name(),
                    request.getMessage(),
                    request.getRequestedAt(),
                    request.getReviewedAt(),
                    request.getRejectionReason()
            );
        }
    }

    public record ApprovedOrgResponse(
            UUID id,
            String name,
            String city,
            String state,
            Instant approvedAt
    ) {}
}
