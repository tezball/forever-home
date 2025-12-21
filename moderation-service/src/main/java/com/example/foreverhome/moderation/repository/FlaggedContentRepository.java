package com.example.foreverhome.moderation.repository;

import com.example.foreverhome.moderation.domain.FlaggedContent;
import com.example.foreverhome.moderation.domain.ModerationCategory;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlaggedContentRepository extends CrudRepository<FlaggedContent, UUID> {

    List<FlaggedContent> findByModerationResultId(UUID moderationResultId);

    List<FlaggedContent> findByCategory(ModerationCategory category);

    @Query("SELECT COUNT(*) FROM flagged_content WHERE category = :category")
    long countByCategory(@Param("category") String category);

    @Query("SELECT COUNT(*) FROM flagged_content WHERE severity = :severity")
    long countBySeverity(@Param("severity") String severity);

    @Query("SELECT category, COUNT(*) as count FROM flagged_content GROUP BY category ORDER BY count DESC")
    List<CategoryCount> countByCategories();

    @Query("DELETE FROM flagged_content WHERE moderation_result_id = :resultId")
    void deleteByModerationResultId(@Param("resultId") UUID resultId);

    interface CategoryCount {
        String getCategory();
        long getCount();
    }
}
