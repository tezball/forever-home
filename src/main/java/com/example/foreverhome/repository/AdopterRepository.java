package com.example.foreverhome.repository;

import com.example.foreverhome.domain.profile.Adopter;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdopterRepository extends CrudRepository<Adopter, UUID> {

    Optional<Adopter> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /**
     * Batch fetch adopters by IDs to avoid N+1 query problems.
     */
    @Query("SELECT * FROM adopters WHERE id IN (:ids)")
    List<Adopter> findAllByIds(@Param("ids") Collection<UUID> ids);
}
