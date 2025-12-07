package com.example.foreverhome.repository;

import com.example.foreverhome.domain.adoption.Adoption;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdoptionRepository extends CrudRepository<Adoption, UUID> {

    Optional<Adoption> findByPetId(UUID petId);

    List<Adoption> findByAdopterId(UUID adopterId);

    List<Adoption> findByFosterId(UUID fosterId);

    List<Adoption> findByRescueOrgId(UUID rescueOrgId);

    @Query("SELECT COUNT(*) FROM adoptions")
    long countAll();

    @Query("SELECT COUNT(*) FROM adoptions WHERE rescue_org_id = :rescueOrgId")
    long countByRescueOrgId(@Param("rescueOrgId") UUID rescueOrgId);
}
