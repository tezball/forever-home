package com.example.foreverhome.repository;

import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.pet.Species;
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

    @Query("SELECT * FROM pets WHERE status = 'AVAILABLE' ORDER BY created_at DESC LIMIT :limit")
    List<Pet> findFeaturedPets(@Param("limit") int limit);
}
