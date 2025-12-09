package com.example.foreverhome.controller;

import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.PetService;
import com.example.foreverhome.service.VetApprovalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    public VetController(PetService petService, VetApprovalService vetApprovalService) {
        this.petService = petService;
        this.vetApprovalService = vetApprovalService;
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
}
