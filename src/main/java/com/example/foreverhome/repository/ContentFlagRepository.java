package com.example.foreverhome.repository;

import com.example.foreverhome.domain.moderation.ContentFlag;
import com.example.foreverhome.domain.moderation.ContentFlag.ContentType;
import com.example.foreverhome.domain.moderation.ContentFlag.FlagStatus;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentFlagRepository extends CrudRepository<ContentFlag, UUID> {

    List<ContentFlag> findByStatus(FlagStatus status);

    @Query("SELECT * FROM content_flags WHERE status = 'PENDING' ORDER BY created_at DESC")
    List<ContentFlag> findPendingFlags();

    @Query("SELECT * FROM content_flags WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<ContentFlag> findByStatusPaged(@Param("status") FlagStatus status,
                                        @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM content_flags ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<ContentFlag> findAllPaged(@Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT COUNT(*) FROM content_flags WHERE status = :status")
    long countByStatus(@Param("status") FlagStatus status);

    List<ContentFlag> findByContentTypeAndContentId(ContentType contentType, UUID contentId);

    @Query("SELECT COUNT(*) FROM content_flags WHERE content_type = :contentType AND content_id = :contentId AND status = 'PENDING'")
    long countPendingByContent(@Param("contentType") ContentType contentType, @Param("contentId") UUID contentId);
}
