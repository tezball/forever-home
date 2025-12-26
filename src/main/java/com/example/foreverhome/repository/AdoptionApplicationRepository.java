package com.example.foreverhome.repository;

import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.domain.adoption.ApplicationStatus;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdoptionApplicationRepository extends CrudRepository<AdoptionApplication, UUID> {

    List<AdoptionApplication> findByPetId(UUID petId);

    /**
     * Find all applications for multiple pets in a single query.
     * Used to avoid N+1 queries when listing applications for a rescue org's pets.
     */
    @Query("SELECT * FROM adoption_applications WHERE pet_id IN (:petIds) ORDER BY submitted_at DESC")
    List<AdoptionApplication> findByPetIdIn(@Param("petIds") List<UUID> petIds);

    List<AdoptionApplication> findByAdopterId(UUID adopterId);

    Optional<AdoptionApplication> findByPetIdAndAdopterId(UUID petId, UUID adopterId);

    @Query("SELECT * FROM adoption_applications WHERE pet_id = :petId AND status IN ('SUBMITTED', 'UNDER_REVIEW')")
    List<AdoptionApplication> findActiveByPetId(@Param("petId") UUID petId);

    @Query("SELECT * FROM adoption_applications WHERE adopter_id = :adopterId AND status IN ('SUBMITTED', 'UNDER_REVIEW')")
    List<AdoptionApplication> findActiveByAdopterId(@Param("adopterId") UUID adopterId);

    @Query("SELECT COUNT(*) FROM adoption_applications WHERE adopter_id = :adopterId AND status IN ('SUBMITTED', 'UNDER_REVIEW')")
    int countActiveByAdopterId(@Param("adopterId") UUID adopterId);

    List<AdoptionApplication> findByStatus(ApplicationStatus status);
}
