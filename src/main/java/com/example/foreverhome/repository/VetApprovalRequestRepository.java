package com.example.foreverhome.repository;

import com.example.foreverhome.domain.verification.VetApprovalRequest;
import com.example.foreverhome.domain.verification.VetApprovalRequestStatus;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VetApprovalRequestRepository extends CrudRepository<VetApprovalRequest, UUID> {

    /**
     * Find all requests made by a specific vet.
     */
    List<VetApprovalRequest> findByVetId(UUID vetId);

    /**
     * Find all requests for a specific rescue organization.
     */
    List<VetApprovalRequest> findByRescueOrgId(UUID rescueOrgId);

    /**
     * Find all requests for a rescue organization with a specific status.
     */
    List<VetApprovalRequest> findByRescueOrgIdAndStatus(UUID rescueOrgId, VetApprovalRequestStatus status);

    /**
     * Find all requests by a vet with a specific status.
     */
    List<VetApprovalRequest> findByVetIdAndStatus(UUID vetId, VetApprovalRequestStatus status);

    /**
     * Find a specific request by vet and rescue org.
     */
    Optional<VetApprovalRequest> findByVetIdAndRescueOrgId(UUID vetId, UUID rescueOrgId);

    /**
     * Check if a pending request exists between a vet and rescue org.
     */
    boolean existsByVetIdAndRescueOrgIdAndStatus(UUID vetId, UUID rescueOrgId, VetApprovalRequestStatus status);

    /**
     * Count pending requests for a rescue organization.
     */
    @Query("SELECT COUNT(*) FROM vet_approval_requests WHERE rescue_org_id = :rescueOrgId AND status = 'PENDING'")
    long countPendingByRescueOrgId(@Param("rescueOrgId") UUID rescueOrgId);

    /**
     * Count pending requests by a vet.
     */
    @Query("SELECT COUNT(*) FROM vet_approval_requests WHERE vet_id = :vetId AND status = 'PENDING'")
    long countPendingByVetId(@Param("vetId") UUID vetId);

    /**
     * Find all pending requests for a rescue organization, ordered by requested_at.
     */
    @Query("SELECT * FROM vet_approval_requests WHERE rescue_org_id = :rescueOrgId AND status = 'PENDING' ORDER BY requested_at ASC")
    List<VetApprovalRequest> findPendingByRescueOrgIdOrderByRequestedAt(@Param("rescueOrgId") UUID rescueOrgId);
}
