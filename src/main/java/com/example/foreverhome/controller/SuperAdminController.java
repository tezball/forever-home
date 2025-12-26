package com.example.foreverhome.controller;

import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.repository.AdoptionRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.UserRepository;
import com.example.foreverhome.repository.VetRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.S3StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller for super admin operations including rescue organization approval
 * and platform-wide statistics.
 */
@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final RescueOrganizationRepository rescueOrganizationRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final AdoptionRepository adoptionRepository;
    private final VetRepository vetRepository;
    private final S3StorageService storageService;

    public SuperAdminController(RescueOrganizationRepository rescueOrganizationRepository,
                                 PetRepository petRepository,
                                 UserRepository userRepository,
                                 AdoptionRepository adoptionRepository,
                                 VetRepository vetRepository,
                                 S3StorageService storageService) {
        this.rescueOrganizationRepository = rescueOrganizationRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.adoptionRepository = adoptionRepository;
        this.vetRepository = vetRepository;
        this.storageService = storageService;
    }

    // ========== Rescue Organization Approval Endpoints ==========

    /**
     * Get all rescue organizations pending verification.
     */
    @GetMapping("/rescues/pending")
    public ResponseEntity<List<RescueOrgAdminResponse>> getPendingRescueOrgs() {
        List<RescueOrganization> pending = rescueOrganizationRepository.findPendingVerification();
        List<RescueOrgAdminResponse> response = pending.stream()
                .map(org -> {
                    String logoUrl = org.getLogoUrl() != null ? storageService.getPublicUrl(org.getLogoUrl()) : null;
                    return RescueOrgAdminResponse.from(org, logoUrl);
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get all rescue organizations (both verified and unverified).
     */
    @GetMapping("/rescues")
    public ResponseEntity<List<RescueOrgAdminResponse>> getAllRescueOrgs() {
        List<RescueOrganization> all = (List<RescueOrganization>) rescueOrganizationRepository.findAll();
        List<RescueOrgAdminResponse> response = all.stream()
                .map(org -> {
                    String logoUrl = org.getLogoUrl() != null ? storageService.getPublicUrl(org.getLogoUrl()) : null;
                    return RescueOrgAdminResponse.from(org, logoUrl);
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a rescue organization.
     */
    @PostMapping("/rescues/{id}/approve")
    public ResponseEntity<RescueOrgAdminResponse> approveRescueOrg(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return rescueOrganizationRepository.findById(id)
                .map(org -> {
                    org.verify();
                    rescueOrganizationRepository.save(org);
                    String logoUrl = org.getLogoUrl() != null ? storageService.getPublicUrl(org.getLogoUrl()) : null;
                    return ResponseEntity.ok(RescueOrgAdminResponse.from(org, logoUrl));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reject/revoke verification for a rescue organization.
     */
    @PostMapping("/rescues/{id}/reject")
    public ResponseEntity<RescueOrgAdminResponse> rejectRescueOrg(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return rescueOrganizationRepository.findById(id)
                .map(org -> {
                    org.unverify();
                    rescueOrganizationRepository.save(org);
                    String logoUrl = org.getLogoUrl() != null ? storageService.getPublicUrl(org.getLogoUrl()) : null;
                    return ResponseEntity.ok(RescueOrgAdminResponse.from(org, logoUrl));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== Platform Statistics Endpoints ==========

    /**
     * Get comprehensive platform statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getAdminStats() {
        // User counts by role
        long totalUsers = userRepository.count();
        long fosterCount = userRepository.countByRole(UserRole.FOSTER);
        long adopterCount = userRepository.countByRole(UserRole.ADOPTER);
        long vetCount = userRepository.countByRole(UserRole.VET);
        long rescueOrgCount = userRepository.countByRole(UserRole.RESCUE_ORG);

        // Rescue org verification counts
        long verifiedRescues = rescueOrganizationRepository.findAllVerified().size();
        long pendingRescues = rescueOrganizationRepository.findPendingVerification().size();

        // Pet counts by status
        long petsAvailable = petRepository.countByStatus(PetStatus.AVAILABLE);
        long petsPendingVet = petRepository.countByStatus(PetStatus.PENDING_VET);
        long petsPendingRescue = petRepository.countByStatus(PetStatus.PENDING_RESCUE);
        long petsInProgress = petRepository.countByStatus(PetStatus.IN_PROGRESS);
        long petsAdopted = petRepository.countByStatus(PetStatus.ADOPTED);
        long petsDraft = petRepository.countByStatus(PetStatus.DRAFT);
        long totalPets = petsAvailable + petsPendingVet + petsPendingRescue + petsInProgress + petsAdopted + petsDraft;

        // Adoption stats
        long totalAdoptions = adoptionRepository.countAll();

        // Verified vets count
        long verifiedVets = vetRepository.findAllVerified().size();

        return ResponseEntity.ok(new AdminStatsResponse(
                new UserStats(totalUsers, fosterCount, adopterCount, vetCount, rescueOrgCount, verifiedVets),
                new RescueStats(rescueOrgCount, verifiedRescues, pendingRescues),
                new PetStats(totalPets, petsAvailable, petsPendingVet, petsPendingRescue, petsInProgress, petsAdopted, petsDraft),
                totalAdoptions,
                Instant.now()
        ));
    }

    // Response DTOs
    record RescueOrgAdminResponse(
            UUID id,
            UUID userId,
            String name,
            String phone,
            String website,
            String description,
            String logoUrl,
            String contactName,
            String contactEmail,
            boolean verified,
            String location
    ) {
        static RescueOrgAdminResponse from(RescueOrganization org, String logoUrl) {
            String location = null;
            if (org.getAddress() != null) {
                String city = org.getAddress().city();
                String state = org.getAddress().state();
                if (city != null && state != null) {
                    location = city + ", " + state;
                } else if (city != null) {
                    location = city;
                } else if (state != null) {
                    location = state;
                }
            }
            return new RescueOrgAdminResponse(
                    org.getId(),
                    org.getUserId(),
                    org.getName(),
                    org.getPhone(),
                    org.getWebsite(),
                    org.getDescription(),
                    logoUrl,
                    org.getContactName(),
                    org.getContactEmail(),
                    org.isVerified(),
                    location
            );
        }
    }

    record AdminStatsResponse(
            UserStats users,
            RescueStats rescues,
            PetStats pets,
            long totalAdoptions,
            Instant generatedAt
    ) {}

    record UserStats(
            long total,
            long fosters,
            long adopters,
            long vets,
            long rescueOrgs,
            long verifiedVets
    ) {}

    record RescueStats(
            long total,
            long verified,
            long pending
    ) {}

    record PetStats(
            long total,
            long available,
            long pendingVet,
            long pendingRescue,
            long inProgress,
            long adopted,
            long draft
    ) {}
}
