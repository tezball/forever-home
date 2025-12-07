package com.example.foreverhome.repository;

import com.example.foreverhome.domain.profile.Foster;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FosterRepository extends CrudRepository<Foster, UUID> {

    Optional<Foster> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
