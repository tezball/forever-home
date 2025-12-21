package com.example.foreverhome.moderation.repository;

import com.example.foreverhome.moderation.domain.ContentType;
import com.example.foreverhome.moderation.domain.ModerationResult;
import com.example.foreverhome.moderation.domain.ModerationStatus;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModerationResultRepository extends CrudRepository<ModerationResult, UUID> {

    List<ModerationResult> findByPetId(UUID petId);

    List<ModerationResult> findByPetIdOrderByCreatedAtDesc(UUID petId);

    List<ModerationResult> findByStatus(ModerationStatus status);

    @Query("SELECT * FROM moderation_results WHERE status = :status ORDER BY created_at DESC LIMIT :limit")
    List<ModerationResult> findByStatusWithLimit(@Param("status") String status, @Param("limit") int limit);

    @Query("SELECT * FROM moderation_results WHERE status = 'FLAGGED' ORDER BY created_at DESC LIMIT :limit")
    List<ModerationResult> findFlaggedWithLimit(@Param("limit") int limit);

    @Query("""
            SELECT * FROM moderation_results mr
            WHERE mr.status = 'FLAGGED'
            AND EXISTS (
                SELECT 1 FROM flagged_content fc
                WHERE fc.moderation_result_id = mr.id
                AND fc.category = :category
            )
            ORDER BY mr.created_at DESC
            LIMIT :limit
            """)
    List<ModerationResult> findFlaggedByCategoryWithLimit(@Param("category") String category, @Param("limit") int limit);

    @Query("SELECT COUNT(*) FROM moderation_results WHERE status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(*) FROM moderation_results WHERE pet_id = :petId")
    long countByPetId(@Param("petId") UUID petId);

    @Query("SELECT COUNT(DISTINCT pet_id) FROM moderation_results WHERE status = 'FLAGGED'")
    long countDistinctFlaggedPets();

    @Query("SELECT COUNT(*) FROM moderation_results WHERE content_type = :contentType")
    long countByContentType(@Param("contentType") String contentType);

    @Query("""
            SELECT * FROM moderation_results
            WHERE pet_id = :petId AND content_type = :contentType
            ORDER BY created_at DESC
            LIMIT 1
            """)
    ModerationResult findLatestByPetIdAndContentType(@Param("petId") UUID petId,
                                                      @Param("contentType") String contentType);

    @Query("DELETE FROM moderation_results WHERE pet_id = :petId")
    void deleteByPetId(@Param("petId") UUID petId);
}
