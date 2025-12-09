package com.example.foreverhome.repository;

import com.example.foreverhome.domain.profile.Vet;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VetRepository extends CrudRepository<Vet, UUID> {

    Optional<Vet> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<Vet> findByVerified(boolean verified);

    @Query("SELECT * FROM vets WHERE verified = true ORDER BY clinic_name")
    List<Vet> findAllVerified();

    @Query("SELECT * FROM vets WHERE verified = false")
    List<Vet> findPendingVerification();

    @Query("SELECT * FROM vets WHERE id NOT IN (SELECT vet_id FROM vet_approvals WHERE rescue_org_id = :rescueOrgId)")
    List<Vet> findNotApprovedByRescueOrg(@Param("rescueOrgId") UUID rescueOrgId);

    @Query("SELECT * FROM vets WHERE id IN (:ids)")
    List<Vet> findByIdIn(@Param("ids") List<UUID> ids);
}
