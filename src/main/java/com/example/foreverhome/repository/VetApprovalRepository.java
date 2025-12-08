package com.example.foreverhome.repository;

import com.example.foreverhome.domain.verification.VetApproval;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VetApprovalRepository extends CrudRepository<VetApproval, UUID> {

    /**
     * Find all approvals for a specific vet.
     */
    List<VetApproval> findByVetId(UUID vetId);

    /**
     * Find all approved vets for a rescue organization.
     */
    List<VetApproval> findByRescueOrgId(UUID rescueOrgId);

    /**
     * Find a specific approval by vet and rescue org.
     */
    Optional<VetApproval> findByVetIdAndRescueOrgId(UUID vetId, UUID rescueOrgId);

    /**
     * Check if a vet is approved by a specific rescue organization.
     */
    boolean existsByVetIdAndRescueOrgId(UUID vetId, UUID rescueOrgId);

    /**
     * Count how many rescue organizations have approved a specific vet.
     */
    @Query("SELECT COUNT(*) FROM vet_approvals WHERE vet_id = :vetId")
    long countByVetId(@Param("vetId") UUID vetId);

    /**
     * Count how many vets a rescue organization has approved.
     */
    @Query("SELECT COUNT(*) FROM vet_approvals WHERE rescue_org_id = :rescueOrgId")
    long countByRescueOrgId(@Param("rescueOrgId") UUID rescueOrgId);

    /**
     * Delete approval (revoke vet access for a rescue org).
     */
    void deleteByVetIdAndRescueOrgId(UUID vetId, UUID rescueOrgId);
}
