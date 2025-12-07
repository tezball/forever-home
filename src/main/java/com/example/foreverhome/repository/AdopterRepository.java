package com.example.foreverhome.repository;

import com.example.foreverhome.domain.profile.Adopter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdopterRepository extends CrudRepository<Adopter, UUID> {

    Optional<Adopter> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
