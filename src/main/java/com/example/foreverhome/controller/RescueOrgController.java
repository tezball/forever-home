package com.example.foreverhome.controller;

import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.verification.VetApproval;
import com.example.foreverhome.domain.verification.VetApprovalRequest;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.VetRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.VetApprovalRequestService;
import com.example.foreverhome.service.VetApprovalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
    private final VetApprovalRequestService vetApprovalRequestService;
    private final RescueOrganizationRepository rescueOrganizationRepository;
    private final VetRepository vetRepository;

    public RescueOrgController(VetApprovalService vetApprovalService,
                               VetApprovalRequestService vetApprovalRequestService,
                               RescueOrganizationRepository rescueOrganizationRepository,
                               VetRepository vetRepository) {
        this.vetApprovalService = vetApprovalService;
        this.vetApprovalRequestService = vetApprovalRequestService;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
        this.vetRepository = vetRepository;
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

    // ========== Vet Approval Request Endpoints ==========

    /**
     * Get all pending approval requests from vets.
     */
    @GetMapping("/approval-requests")
    public ResponseEntity<List<VetApprovalRequestResponse>> getPendingApprovalRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<VetApprovalRequest> requests = vetApprovalRequestService.getPendingRequestsForRescueOrg(principal.userId());
        List<VetApprovalRequestResponse> response = requests.stream()
                .map(request -> {
                    Vet vet = vetRepository.findById(request.getVetId()).orElse(null);
                    return VetApprovalRequestResponse.from(request, vet);
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get the count of pending approval requests.
     */
    @GetMapping("/approval-requests/count")
    public ResponseEntity<ApprovalRequestCountResponse> getPendingApprovalRequestCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        long count = vetApprovalRequestService.countPendingRequestsForRescueOrg(principal.userId());
        return ResponseEntity.ok(new ApprovalRequestCountResponse(count));
    }

    /**
     * Approve a vet's approval request.
     */
    @PostMapping("/approval-requests/{requestId}/approve")
    public ResponseEntity<VetApprovalResponse> approveApprovalRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        VetApproval approval = vetApprovalRequestService.approveRequest(principal.userId(), requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(VetApprovalResponse.from(approval));
    }

    /**
     * Reject a vet's approval request.
     */
    @PostMapping("/approval-requests/{requestId}/reject")
    public ResponseEntity<Void> rejectApprovalRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) RejectRequestBody requestBody,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = requestBody != null ? requestBody.reason() : null;
        vetApprovalRequestService.rejectRequest(principal.userId(), requestId, reason);
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

    // Request DTOs
    record RejectRequestBody(
            String reason
    ) {}

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

    record VetApprovalRequestResponse(
            UUID id,
            UUID vetId,
            String vetClinicName,
            String vetLicenseNumber,
            String vetPhone,
            String vetCity,
            String vetState,
            String status,
            String message,
            Instant requestedAt
    ) {
        static VetApprovalRequestResponse from(VetApprovalRequest request, Vet vet) {
            String city = vet != null && vet.getAddress() != null ? vet.getAddress().city() : null;
            String state = vet != null && vet.getAddress() != null ? vet.getAddress().state() : null;
            return new VetApprovalRequestResponse(
                    request.getId(),
                    request.getVetId(),
                    vet != null ? vet.getClinicName() : "Unknown",
                    vet != null ? vet.getLicenseNumber() : null,
                    vet != null ? vet.getPhone() : null,
                    city,
                    state,
                    request.getStatus().name(),
                    request.getMessage(),
                    request.getRequestedAt()
            );
        }
    }

    record ApprovalRequestCountResponse(long count) {}
}
