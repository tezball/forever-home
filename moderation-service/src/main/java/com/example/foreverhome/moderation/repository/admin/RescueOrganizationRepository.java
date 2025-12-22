package com.example.foreverhome.moderation.repository.admin;

import com.example.foreverhome.moderation.domain.admin.RescueOrganization;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RescueOrganizationRepository extends CrudRepository<RescueOrganization, UUID> {

    Optional<RescueOrganization> findByUserId(UUID userId);
}
