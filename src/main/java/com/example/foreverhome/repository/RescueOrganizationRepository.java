package com.example.foreverhome.repository;

import com.example.foreverhome.domain.profile.RescueOrganization;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RescueOrganizationRepository extends CrudRepository<RescueOrganization, UUID> {

    Optional<RescueOrganization> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<RescueOrganization> findByVerified(boolean verified);

    @Query("SELECT * FROM rescue_organizations WHERE verified = true ORDER BY name")
    List<RescueOrganization> findAllVerified();

    @Query("SELECT * FROM rescue_organizations WHERE verified = false")
    List<RescueOrganization> findPendingVerification();
}
