package com.example.foreverhome.repository;

import com.example.foreverhome.domain.verification.VetSignOff;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VetSignOffRepository extends CrudRepository<VetSignOff, UUID> {

    Optional<VetSignOff> findByPetId(UUID petId);

    List<VetSignOff> findByVetId(UUID vetId);

    @Query("SELECT * FROM vet_signoffs WHERE vet_id = :vetId ORDER BY signed_off_at DESC")
    List<VetSignOff> findByVetIdOrderBySignedOffAtDesc(@Param("vetId") UUID vetId);

    @Query("SELECT COUNT(*) FROM vet_signoffs WHERE vet_id = :vetId")
    long countByVetId(@Param("vetId") UUID vetId);

    boolean existsByPetId(UUID petId);
}
