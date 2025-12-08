package com.example.foreverhome.controller;

import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.verification.VetApproval;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.VetApprovalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for rescue organization operations, including vet approval management.
 */
@RestController
@RequestMapping("/api/rescue-org")
@PreAuthorize("hasRole('RESCUE_ORG')")
public class RescueOrgController {

    private final VetApprovalService vetApprovalService;
    private final RescueOrganizationRepository rescueOrganizationRepository;

    public RescueOrgController(VetApprovalService vetApprovalService,
                               RescueOrganizationRepository rescueOrganizationRepository) {
        this.vetApprovalService = vetApprovalService;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
    }

    /**
     * Get all vets approved by this rescue organization.
     */
    @GetMapping("/vets/approved")
    public ResponseEntity<List<VetResponse>> getApprovedVets(@AuthenticationPrincipal UserPrincipal principal) {
        List<Vet> vets = vetApprovalService.getApprovedVetsForRescueOrg(principal.userId());
        List<VetResponse> response = vets.stream()
                .map(VetResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get all vets that can be approved (not yet approved by this rescue).
     */
    @GetMapping("/vets/pending")
    public ResponseEntity<List<VetResponse>> getPendingVets(@AuthenticationPrincipal UserPrincipal principal) {
        List<Vet> vets = vetApprovalService.getPendingVetsForRescueOrg(principal.userId());
        List<VetResponse> response = vets.stream()
                .map(VetResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a vet to verify pets for this rescue organization.
     */
    @PostMapping("/vets/{vetId}/approve")
    public ResponseEntity<VetApprovalResponse> approveVet(
            @PathVariable UUID vetId,
            @AuthenticationPrincipal UserPrincipal principal) {
        VetApproval approval = vetApprovalService.approveVet(principal.userId(), vetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(VetApprovalResponse.from(approval));
    }

    /**
     * Revoke a vet's approval.
     */
    @DeleteMapping("/vets/{vetId}/approve")
    public ResponseEntity<Void> revokeVetApproval(
            @PathVariable UUID vetId,
            @AuthenticationPrincipal UserPrincipal principal) {
        vetApprovalService.revokeVetApproval(principal.userId(), vetId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get list of all verified rescue organizations (public endpoint for vets to discover rescues).
     */
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RescueOrgResponse>> listVerifiedRescueOrgs() {
        List<RescueOrganization> orgs = rescueOrganizationRepository.findAllVerified();
        List<RescueOrgResponse> response = orgs.stream()
                .map(RescueOrgResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    // Response DTOs
    record VetResponse(
            UUID id,
            String clinicName,
            String licenseNumber,
            String phone,
            String website,
            String description,
            String city,
            String state,
            boolean verified
    ) {
        static VetResponse from(Vet vet) {
            String city = vet.getAddress() != null ? vet.getAddress().city() : null;
            String state = vet.getAddress() != null ? vet.getAddress().state() : null;
            return new VetResponse(
                    vet.getId(),
                    vet.getClinicName(),
                    vet.getLicenseNumber(),
                    vet.getPhone(),
                    vet.getWebsite(),
                    vet.getDescription(),
                    city,
                    state,
                    vet.isVerified()
            );
        }
    }

    record VetApprovalResponse(
            UUID id,
            UUID vetId,
            UUID rescueOrgId,
            String approvedAt
    ) {
        static VetApprovalResponse from(VetApproval approval) {
            return new VetApprovalResponse(
                    approval.getId(),
                    approval.getVetId(),
                    approval.getRescueOrgId(),
                    approval.getApprovedAt().toString()
            );
        }
    }

    record RescueOrgResponse(
            UUID id,
            String name,
            String city,
            String state,
            String phone,
            String website,
            String description,
            String logoUrl
    ) {
        static RescueOrgResponse from(RescueOrganization org) {
            String city = org.getAddress() != null ? org.getAddress().city() : null;
            String state = org.getAddress() != null ? org.getAddress().state() : null;
            return new RescueOrgResponse(
                    org.getId(),
                    org.getName(),
                    city,
                    state,
                    org.getPhone(),
                    org.getWebsite(),
                    org.getDescription(),
                    org.getLogoUrl()
            );
        }
    }
}
