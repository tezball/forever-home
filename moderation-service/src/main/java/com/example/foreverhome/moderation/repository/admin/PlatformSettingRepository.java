package com.example.foreverhome.moderation.repository.admin;

import com.example.foreverhome.moderation.domain.admin.PlatformSetting;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformSettingRepository extends CrudRepository<PlatformSetting, UUID> {

    Optional<PlatformSetting> findBySettingKey(String settingKey);

    @Query("SELECT * FROM platform_settings WHERE category = :category ORDER BY setting_key")
    List<PlatformSetting> findByCategory(@Param("category") String category);

    @Query("SELECT * FROM platform_settings ORDER BY category, setting_key")
    List<PlatformSetting> findAllOrderByCategoryAndKey();

    @Query("SELECT DISTINCT category FROM platform_settings ORDER BY category")
    List<String> findAllCategories();

    boolean existsBySettingKey(String settingKey);
}
