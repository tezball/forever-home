package com.example.foreverhome.moderation.repository.admin;

import com.example.foreverhome.moderation.domain.admin.FeatureFlag;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureFlagRepository extends CrudRepository<FeatureFlag, UUID> {

    Optional<FeatureFlag> findByFlagKey(String flagKey);

    @Query("SELECT * FROM feature_flags ORDER BY flag_key")
    List<FeatureFlag> findAllOrderByFlagKey();

    @Query("SELECT * FROM feature_flags WHERE enabled = true ORDER BY flag_key")
    List<FeatureFlag> findAllEnabled();

    @Query("SELECT * FROM feature_flags WHERE enabled = false ORDER BY flag_key")
    List<FeatureFlag> findAllDisabled();

    boolean existsByFlagKey(String flagKey);
}
