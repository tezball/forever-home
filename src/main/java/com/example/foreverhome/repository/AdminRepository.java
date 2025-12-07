package com.example.foreverhome.repository;

import com.example.foreverhome.domain.profile.Admin;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends CrudRepository<Admin, UUID> {

    Optional<Admin> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
