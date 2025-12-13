package com.example.foreverhome.service;

import com.example.foreverhome.controller.VetController.ApprovedOrgResponse;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.verification.VetApproval;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.VetApprovalRepository;
import com.example.foreverhome.repository.VetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing vet approvals by rescue organizations.
 * Rescue organizations must approve vets before they can verify pets for that organization.
 */
@Service
@Transactional
public class VetApprovalService {

    private final VetApprovalRepository vetApprovalRepository;
    private final VetRepository vetRepository;
    private final RescueOrganizationRepository rescueOrganizationRepository;
    private final NotificationService notificationService;

    public VetApprovalService(VetApprovalRepository vetApprovalRepository,
                              VetRepository vetRepository,
                              RescueOrganizationRepository rescueOrganizationRepository,
                              NotificationService notificationService) {
        this.vetApprovalRepository = vetApprovalRepository;
        this.vetRepository = vetRepository;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
        this.notificationService = notificationService;
    }

    /**
     * Approve a vet for a rescue organization.
     *
     * @param rescueOrgUserId The user ID of the rescue organization representative
     * @param vetId           The ID of the vet to approve
     * @return The created VetApproval
     */
    public VetApproval approveVet(UUID rescueOrgUserId, UUID vetId) {
        // Find rescue org by user ID
        RescueOrganization rescueOrg = rescueOrganizationRepository.findByUserId(rescueOrgUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization not found for user"));

        // Verify rescue org is verified
        if (!rescueOrg.isVerified()) {
            throw new AccessDeniedException("Your organization must be verified before approving vets");
        }

        // Verify vet exists
        Vet vet = vetRepository.findById(vetId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet", vetId));

        // Check if already approved
        if (vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrg.getId())) {
            return vetApprovalRepository.findByVetIdAndRescueOrgId(vetId, rescueOrg.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("VetApproval", "vetId and rescueOrgId"));
        }

        // Create approval
        VetApproval approval = VetApproval.create(vetId, rescueOrg.getId(), rescueOrgUserId);
        VetApproval saved = vetApprovalRepository.save(approval);

        // Also mark the vet as verified (global flag indicates at least one rescue has approved)
        if (!vet.isVerified()) {
            vet.verify();
            vetRepository.save(vet);
        }

        // Notify vet
        notificationService.notifyVetApproved(vet.getUserId(), rescueOrg.getName());

        return saved;
    }

    /**
     * Revoke a vet's approval for a rescue organization.
     *
     * @param rescueOrgUserId The user ID of the rescue organization representative
     * @param vetId           The ID of the vet to revoke
     */
    public void revokeVetApproval(UUID rescueOrgUserId, UUID vetId) {
        // Find rescue org by user ID
        RescueOrganization rescueOrg = rescueOrganizationRepository.findByUserId(rescueOrgUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization not found for user"));

        // Delete the approval
        vetApprovalRepository.deleteByVetIdAndRescueOrgId(vetId, rescueOrg.getId());

        // Check if vet still has any approvals
        long remainingApprovals = vetApprovalRepository.countByVetId(vetId);
        if (remainingApprovals == 0) {
            // Unverify the vet if no more approvals
            vetRepository.findById(vetId).ifPresent(vet -> {
                vet.unverify();
                vetRepository.save(vet);
            });
        }
    }

    /**
     * Get all vets approved by a rescue organization.
     */
    @Transactional(readOnly = true)
    public List<Vet> getApprovedVetsForRescueOrg(UUID rescueOrgUserId) {
        RescueOrganization rescueOrg = rescueOrganizationRepository.findByUserId(rescueOrgUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization not found for user"));

        List<VetApproval> approvals = vetApprovalRepository.findByRescueOrgId(rescueOrg.getId());

        if (approvals.isEmpty()) {
            return List.of();
        }

        // Batch fetch all vets in one query to avoid N+1
        List<UUID> vetIds = approvals.stream()
                .map(VetApproval::getVetId)
                .toList();
        return vetRepository.findByIdIn(vetIds);
    }

    /**
     * Get all rescue organizations that have approved a vet.
     */
    @Transactional(readOnly = true)
    public List<RescueOrganization> getRescueOrgsForVet(UUID vetUserId) {
        Vet vet = vetRepository.findByUserId(vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found for user"));

        List<VetApproval> approvals = vetApprovalRepository.findByVetId(vet.getId());
        return approvals.stream()
                .map(approval -> rescueOrganizationRepository.findById(approval.getRescueOrgId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }

    /**
     * Get all approved organizations with approval dates for a vet (for dashboard display).
     */
    @Transactional(readOnly = true)
    public List<ApprovedOrgResponse> getApprovedOrgsWithDatesForVet(UUID vetUserId) {
        Vet vet = vetRepository.findByUserId(vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found for user"));

        List<VetApproval> approvals = vetApprovalRepository.findByVetId(vet.getId());
        return approvals.stream()
                .map(approval -> {
                    RescueOrganization org = rescueOrganizationRepository.findById(approval.getRescueOrgId())
                            .orElse(null);
                    if (org == null) return null;
                    String city = org.getAddress() != null ? org.getAddress().city() : null;
                    String state = org.getAddress() != null ? org.getAddress().state() : null;
                    return new ApprovedOrgResponse(
                            org.getId(),
                            org.getName(),
                            city,
                            state,
                            approval.getApprovedAt()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Get all pending (not yet approved) vets.
     * Returns vets who have completed their profile but are not approved by this rescue org.
     */
    @Transactional(readOnly = true)
    public List<Vet> getPendingVetsForRescueOrg(UUID rescueOrgUserId) {
        RescueOrganization rescueOrg = rescueOrganizationRepository.findByUserId(rescueOrgUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization not found for user"));

        // Use efficient single query instead of loading all vets
        return vetRepository.findNotApprovedByRescueOrg(rescueOrg.getId());
    }

    /**
     * Check if a vet is approved by a specific rescue organization.
     *
     * @param vetId       The vet's ID
     * @param rescueOrgId The rescue organization's ID
     * @return true if the vet is approved by the rescue org
     */
    @Transactional(readOnly = true)
    public boolean isVetApprovedByRescueOrg(UUID vetId, UUID rescueOrgId) {
        return vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrgId);
    }

    /**
     * Check if a vet (by user ID) is approved by a specific rescue organization.
     */
    @Transactional(readOnly = true)
    public boolean isVetUserApprovedByRescueOrg(UUID vetUserId, UUID rescueOrgId) {
        return vetRepository.findByUserId(vetUserId)
                .map(vet -> vetApprovalRepository.existsByVetIdAndRescueOrgId(vet.getId(), rescueOrgId))
                .orElse(false);
    }
}
