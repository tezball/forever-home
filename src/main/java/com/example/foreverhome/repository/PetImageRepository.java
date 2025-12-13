package com.example.foreverhome.repository;

import com.example.foreverhome.domain.pet.PetImage;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetImageRepository extends CrudRepository<PetImage, UUID> {

    List<PetImage> findByPetIdOrderByDisplayOrder(UUID petId);

    @Query("SELECT * FROM pet_images WHERE pet_id = :petId AND is_primary = true")
    Optional<PetImage> findPrimaryByPetId(@Param("petId") UUID petId);

    @Query("SELECT * FROM pet_images WHERE pet_id IN (:petIds) AND is_primary = true")
    List<PetImage> findPrimaryImagesByPetIds(@Param("petIds") List<UUID> petIds);

    @Query("SELECT COUNT(*) FROM pet_images WHERE pet_id = :petId")
    int countByPetId(@Param("petId") UUID petId);

    @Modifying
    @Query("DELETE FROM pet_images WHERE pet_id = :petId")
    void deleteByPetId(@Param("petId") UUID petId);

    @Modifying
    @Query("UPDATE pet_images SET is_primary = false WHERE pet_id = :petId")
    void clearPrimaryByPetId(@Param("petId") UUID petId);
}
