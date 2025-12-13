package com.example.foreverhome.service;

import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.verification.VetApproval;
import com.example.foreverhome.domain.verification.VetApprovalRequest;
import com.example.foreverhome.domain.verification.VetApprovalRequestStatus;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.VetApprovalRepository;
import com.example.foreverhome.repository.VetApprovalRequestRepository;
import com.example.foreverhome.repository.VetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing vet approval requests.
 * Allows vets to request approval from rescue organizations,
 * and rescue organizations to approve or reject those requests.
 */
@Service
@Transactional
public class VetApprovalRequestService {

    private final VetApprovalRequestRepository requestRepository;
    private final VetApprovalRepository approvalRepository;
    private final VetRepository vetRepository;
    private final RescueOrganizationRepository rescueOrganizationRepository;
    private final NotificationService notificationService;

    public VetApprovalRequestService(VetApprovalRequestRepository requestRepository,
                                      VetApprovalRepository approvalRepository,
                                      VetRepository vetRepository,
                                      RescueOrganizationRepository rescueOrganizationRepository,
                                      NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.approvalRepository = approvalRepository;
        this.vetRepository = vetRepository;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
        this.notificationService = notificationService;
    }

    /**
     * Submit an approval request from a vet to a rescue organization.
     *
     * @param vetUserId   The user ID of the vet requesting approval
     * @param rescueOrgId The ID of the rescue organization
     * @param message     Optional message from the vet
     * @return The created VetApprovalRequest
     */
    public VetApprovalRequest requestApproval(UUID vetUserId, UUID rescueOrgId, String message) {
        // Find vet by user ID
        Vet vet = vetRepository.findByUserId(vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found for user"));

        // Find rescue org
        RescueOrganization rescueOrg = rescueOrganizationRepository.findById(rescueOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("RescueOrganization", rescueOrgId));

        // Verify rescue org is verified
        if (!rescueOrg.isVerified()) {
            throw new IllegalArgumentException("Cannot request approval from an unverified organization");
        }

        // Check if already approved
        if (approvalRepository.existsByVetIdAndRescueOrgId(vet.getId(), rescueOrgId)) {
            throw new IllegalArgumentException("You are already approved by this organization");
        }

        // Check if there's already a pending request
        if (requestRepository.existsByVetIdAndRescueOrgIdAndStatus(vet.getId(), rescueOrgId, VetApprovalRequestStatus.PENDING)) {
            throw new IllegalArgumentException("You already have a pending request with this organization");
        }

        // Create the request
        VetApprovalRequest request = VetApprovalRequest.create(vet.getId(), rescueOrgId, message);
        VetApprovalRequest saved = requestRepository.save(request);

        // Notify rescue org
        notificationService.notifyRescueOrgVetRequest(rescueOrg.getUserId(), vet.getClinicName());

        return saved;
    }

    /**
     * Approve a vet's request. Creates a VetApproval record.
     *
     * @param rescueOrgUserId The user ID of the rescue organization
     * @param requestId       The ID of the request to approve
     * @return The created VetApproval
     */
    public VetApproval approveRequest(UUID rescueOrgUserId, UUID requestId) {
        // Find rescue org by user ID
        RescueOrganization rescueOrg = rescueOrganizationRepository.findByUserId(rescueOrgUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization not found for user"));

        // Find request
        VetApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("VetApprovalRequest", requestId));

        // Verify request belongs to this rescue org
        if (!request.getRescueOrgId().equals(rescueOrg.getId())) {
            throw new AccessDeniedException("This request does not belong to your organization");
        }

        // Verify request is pending
        if (!request.canApprove()) {
            throw new IllegalArgumentException("Request cannot be approved with status: " + request.getStatus());
        }

        // Approve the request
        request.approve(rescueOrgUserId);
        requestRepository.save(request);

        // Create the VetApproval record
        VetApproval approval = VetApproval.create(request.getVetId(), rescueOrg.getId(), rescueOrgUserId);
        VetApproval savedApproval = approvalRepository.save(approval);

        // Mark vet as verified if first approval
        Vet vet = vetRepository.findById(request.getVetId())
                .orElseThrow(() -> new ResourceNotFoundException("Vet", request.getVetId()));
        if (!vet.isVerified()) {
            vet.verify();
            vetRepository.save(vet);
        }

        // Notify vet
        notificationService.notifyVetRequestApproved(vet.getUserId(), rescueOrg.getName());

        return savedApproval;
    }

    /**
     * Reject a vet's request.
     *
     * @param rescueOrgUserId The user ID of the rescue organization
     * @param requestId       The ID of the request to reject
     * @param reason          Optional reason for rejection
     */
    public void rejectRequest(UUID rescueOrgUserId, UUID requestId, String reason) {
        // Find rescue org by user ID
        RescueOrganization rescueOrg = rescueOrganizationRepository.findByUserId(rescueOrgUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization not found for user"));

        // Find request
        VetApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("VetApprovalRequest", requestId));

        // Verify request belongs to this rescue org
        if (!request.getRescueOrgId().equals(rescueOrg.getId())) {
            throw new AccessDeniedException("This request does not belong to your organization");
        }

        // Verify request can be rejected
        if (!request.canReject()) {
            throw new IllegalArgumentException("Request cannot be rejected with status: " + request.getStatus());
        }

        // Reject the request
        request.reject(rescueOrgUserId, reason);
        requestRepository.save(request);

        // Notify vet
        Vet vet = vetRepository.findById(request.getVetId())
                .orElseThrow(() -> new ResourceNotFoundException("Vet", request.getVetId()));
        notificationService.notifyVetRequestRejected(vet.getUserId(), rescueOrg.getName(), reason);
    }

    /**
     * Withdraw a pending request.
     *
     * @param vetUserId The user ID of the vet
     * @param requestId The ID of the request to withdraw
     */
    public void withdrawRequest(UUID vetUserId, UUID requestId) {
        // Find vet by user ID
        Vet vet = vetRepository.findByUserId(vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found for user"));

        // Find request
        VetApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("VetApprovalRequest", requestId));

        // Verify request belongs to this vet
        if (!request.getVetId().equals(vet.getId())) {
            throw new AccessDeniedException("This request does not belong to you");
        }

        // Verify request can be withdrawn
        if (!request.canWithdraw()) {
            throw new IllegalArgumentException("Request cannot be withdrawn with status: " + request.getStatus());
        }

        // Withdraw the request
        request.withdraw();
        requestRepository.save(request);
    }

    /**
     * Get all pending requests for a rescue organization.
     */
    @Transactional(readOnly = true)
    public List<VetApprovalRequest> getPendingRequestsForRescueOrg(UUID rescueOrgUserId) {
        RescueOrganization rescueOrg = rescueOrganizationRepository.findByUserId(rescueOrgUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization not found for user"));

        return requestRepository.findPendingByRescueOrgIdOrderByRequestedAt(rescueOrg.getId());
    }

    /**
     * Get all requests made by a vet.
     */
    @Transactional(readOnly = true)
    public List<VetApprovalRequest> getRequestsForVet(UUID vetUserId) {
        Vet vet = vetRepository.findByUserId(vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found for user"));

        return requestRepository.findByVetId(vet.getId());
    }

    /**
     * Get a request by ID, verifying the user has access.
     */
    @Transactional(readOnly = true)
    public VetApprovalRequest getRequestById(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("VetApprovalRequest", requestId));
    }

    /**
     * Get all verified rescue organizations that a vet can request approval from.
     * Excludes organizations where the vet is already approved or has a pending request.
     */
    @Transactional(readOnly = true)
    public List<RescueOrganization> getAvailableRescueOrgsForVet(UUID vetUserId) {
        Vet vet = vetRepository.findByUserId(vetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found for user"));

        // Get all verified rescue orgs
        List<RescueOrganization> verifiedOrgs = rescueOrganizationRepository.findAllVerified();

        // Filter out orgs where vet is already approved or has pending request
        return verifiedOrgs.stream()
                .filter(org -> !approvalRepository.existsByVetIdAndRescueOrgId(vet.getId(), org.getId()))
                .filter(org -> !requestRepository.existsByVetIdAndRescueOrgIdAndStatus(
                        vet.getId(), org.getId(), VetApprovalRequestStatus.PENDING))
                .toList();
    }

    /**
     * Count pending requests for a rescue organization.
     */
    @Transactional(readOnly = true)
    public long countPendingRequestsForRescueOrg(UUID rescueOrgUserId) {
        RescueOrganization rescueOrg = rescueOrganizationRepository.findByUserId(rescueOrgUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization not found for user"));

        return requestRepository.countPendingByRescueOrgId(rescueOrg.getId());
    }
}
