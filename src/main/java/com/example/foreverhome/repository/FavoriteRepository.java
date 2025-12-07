package com.example.foreverhome.repository;

import com.example.foreverhome.domain.adoption.Favorite;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends CrudRepository<Favorite, UUID> {

    List<Favorite> findByAdopterId(UUID adopterId);

    List<Favorite> findByPetId(UUID petId);

    Optional<Favorite> findByAdopterIdAndPetId(UUID adopterId, UUID petId);

    boolean existsByAdopterIdAndPetId(UUID adopterId, UUID petId);

    @Modifying
    @Query("DELETE FROM favorites WHERE adopter_id = :adopterId AND pet_id = :petId")
    void deleteByAdopterIdAndPetId(@Param("adopterId") UUID adopterId, @Param("petId") UUID petId);
}
