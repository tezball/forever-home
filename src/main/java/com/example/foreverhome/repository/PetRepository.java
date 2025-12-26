package com.example.foreverhome.repository;

import com.example.foreverhome.domain.pet.ModerationStatus;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.pet.Species;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetRepository extends CrudRepository<Pet, UUID> {

    Optional<Pet> findByMicrochipId(String microchipId);

    boolean existsByMicrochipId(String microchipId);

    List<Pet> findByStatus(PetStatus status);

    List<Pet> findByFosterId(UUID fosterId);

    List<Pet> findByRescueOrgId(UUID rescueOrgId);

    @Query("SELECT * FROM pets WHERE status = 'AVAILABLE' ORDER BY created_at DESC")
    List<Pet> findAllAvailable();

    @Query("""
        SELECT * FROM pets
        WHERE status = 'AVAILABLE'
          AND (:species IS NULL OR species = :species)
          AND (:size IS NULL OR size = :size)
          AND (:sex IS NULL OR sex = :sex)
          AND (:minAge IS NULL OR age >= :minAge)
          AND (:maxAge IS NULL OR age <= :maxAge)
        ORDER BY created_at DESC
        """)
    List<Pet> findAvailableWithFilters(
            @Param("species") String species,
            @Param("size") String size,
            @Param("sex") String sex,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge
    );

    @Query("SELECT * FROM pets WHERE status IN ('AVAILABLE', 'IN_PROGRESS', 'ON_HOLD') ORDER BY created_at DESC")
    List<Pet> findAllPubliclyVisible();

    @Query("SELECT * FROM pets WHERE rescue_org_id = :rescueOrgId AND status = :status")
    List<Pet> findByRescueOrgIdAndStatus(@Param("rescueOrgId") UUID rescueOrgId, @Param("status") PetStatus status);

    @Query("SELECT * FROM pets WHERE species = :species AND status = 'AVAILABLE'")
    List<Pet> findAvailableBySpecies(@Param("species") Species species);

    @Query("SELECT COUNT(*) FROM pets WHERE status = :status")
    long countByStatus(@Param("status") PetStatus status);

    @Query("SELECT * FROM pets WHERE rescue_org_id = :rescueOrgId AND status = 'PENDING_RESCUE' ORDER BY created_at DESC")
    List<Pet> findPendingByRescueOrgId(@Param("rescueOrgId") UUID rescueOrgId);

    @Query("SELECT * FROM pets WHERE id IN (:ids)")
    List<Pet> findByIdIn(@Param("ids") List<UUID> ids);

    @Query("SELECT COUNT(*) FROM pets WHERE rescue_org_id = :rescueOrgId AND status = :status")
    long countByRescueOrgIdAndStatus(@Param("rescueOrgId") UUID rescueOrgId, @Param("status") PetStatus status);

    /**
     * Count pets by status grouped by rescue organization ID.
     * Used to avoid N+1 queries when listing rescue organizations with pet counts.
     */
    @Query("SELECT rescue_org_id, COUNT(*) as pet_count FROM pets WHERE status = :status AND rescue_org_id IN (:rescueOrgIds) GROUP BY rescue_org_id")
    List<RescueOrgPetCount> countByStatusGroupedByRescueOrgId(@Param("status") PetStatus status, @Param("rescueOrgIds") List<UUID> rescueOrgIds);

    /**
     * Projection for rescue org pet count results.
     */
    interface RescueOrgPetCount {
        UUID getRescueOrgId();
        long getPetCount();
    }

    @Query("SELECT * FROM pets WHERE status = 'AVAILABLE' ORDER BY created_at DESC LIMIT :limit")
    List<Pet> findFeaturedPets(@Param("limit") int limit);

    @Query("SELECT * FROM pets WHERE status = 'AVAILABLE' ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<Pet> findFeaturedPetsPageable(@Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM pets WHERE status = :status AND rescue_org_id IN (:rescueOrgIds) ORDER BY created_at DESC")
    List<Pet> findByStatusAndRescueOrgIdIn(@Param("status") PetStatus status, @Param("rescueOrgIds") List<UUID> rescueOrgIds);

    /**
     * Find pets created directly by a rescue organization (no foster involved).
     * Rescue-owned pets have foster_id = NULL.
     */
    @Query("SELECT * FROM pets WHERE rescue_org_id = :rescueOrgId AND foster_id IS NULL ORDER BY created_at DESC")
    List<Pet> findByRescueOrgIdAndFosterIdIsNull(@Param("rescueOrgId") UUID rescueOrgId);

    // Paginated queries
    @Query("""
        SELECT * FROM pets
        WHERE status = 'AVAILABLE'
          AND (:species IS NULL OR species = :species)
          AND (:size IS NULL OR size = :size)
          AND (:sex IS NULL OR sex = :sex)
          AND (:minAge IS NULL OR age >= :minAge)
          AND (:maxAge IS NULL OR age <= :maxAge)
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
        """)
    List<Pet> findAvailableWithFiltersPageable(
            @Param("species") String species,
            @Param("size") String size,
            @Param("sex") String sex,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query("""
        SELECT COUNT(*) FROM pets
        WHERE status = 'AVAILABLE'
          AND (:species IS NULL OR species = :species)
          AND (:size IS NULL OR size = :size)
          AND (:sex IS NULL OR sex = :sex)
          AND (:minAge IS NULL OR age >= :minAge)
          AND (:maxAge IS NULL OR age <= :maxAge)
        """)
    long countAvailableWithFilters(
            @Param("species") String species,
            @Param("size") String size,
            @Param("sex") String sex,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge
    );

    // Moderation status queries

    /**
     * Find all pets with the given moderation status.
     */
    List<Pet> findByModerationStatus(ModerationStatus moderationStatus);

    /**
     * Count pets by moderation status.
     */
    @Query("SELECT COUNT(*) FROM pets WHERE moderation_status = :status")
    long countByModerationStatus(@Param("status") String status);

    /**
     * Find pets that need moderation review (FLAGGED status).
     */
    @Query("SELECT * FROM pets WHERE moderation_status = 'FLAGGED' ORDER BY moderated_at DESC")
    List<Pet> findPetsFlaggedForReview();

    /**
     * Find pets pending moderation check.
     */
    @Query("SELECT * FROM pets WHERE moderation_status = 'PENDING' ORDER BY created_at ASC")
    List<Pet> findPetsPendingModeration();

    /**
     * Reset all pet moderation statuses to PENDING.
     * Used to re-queue all pets for AI moderation after system changes.
     * Returns the number of pets updated.
     */
    @Modifying
    @Query("UPDATE pets SET moderation_status = 'PENDING', moderated_at = NULL, moderation_notes = NULL")
    int resetAllModerationStatuses();
}
