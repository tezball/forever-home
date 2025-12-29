package com.example.foreverhome.repository;

import com.example.foreverhome.domain.pet.PetStatusHistory;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetStatusHistoryRepository extends CrudRepository<PetStatusHistory, UUID> {

    @Query("SELECT * FROM pet_status_history WHERE pet_id = :petId ORDER BY changed_at DESC")
    List<PetStatusHistory> findByPetIdOrderByChangedAtDesc(@Param("petId") UUID petId);

    @Query("SELECT * FROM pet_status_history WHERE pet_id = :petId AND to_status = :status ORDER BY changed_at DESC LIMIT 1")
    Optional<PetStatusHistory> findLatestByPetIdAndNewStatus(@Param("petId") UUID petId, @Param("status") String status);
}
