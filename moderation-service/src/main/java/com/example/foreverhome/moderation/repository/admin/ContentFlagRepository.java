package com.example.foreverhome.moderation.repository.admin;

import com.example.foreverhome.moderation.domain.admin.ContentFlag;
import com.example.foreverhome.moderation.domain.admin.ContentFlag.FlagStatus;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentFlagRepository extends CrudRepository<ContentFlag, UUID> {

    @Query("SELECT * FROM content_flags ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<ContentFlag> findAllPaged(@Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM content_flags WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<ContentFlag> findByStatusPaged(@Param("status") FlagStatus status, @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT COUNT(*) FROM content_flags WHERE status = :status")
    long countByStatus(@Param("status") FlagStatus status);

    @Query("SELECT COUNT(*) FROM content_flags WHERE status = 'PENDING'")
    long countPending();
}
