package com.example.foreverhome.repository;

import com.example.foreverhome.domain.moderation.AuditLog;
import com.example.foreverhome.domain.moderation.AuditLog.AuditAction;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends CrudRepository<AuditLog, UUID> {

    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<AuditLog> findAllPaged(@Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM audit_logs WHERE actor_id = :actorId ORDER BY created_at DESC LIMIT :limit")
    List<AuditLog> findByActorId(@Param("actorId") UUID actorId, @Param("limit") int limit);

    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY created_at DESC LIMIT :limit")
    List<AuditLog> findByAction(@Param("action") AuditAction action, @Param("limit") int limit);

    @Query("SELECT * FROM audit_logs WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY created_at DESC")
    List<AuditLog> findByEntity(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    @Query("SELECT * FROM audit_logs WHERE created_at >= :since ORDER BY created_at DESC LIMIT :limit")
    List<AuditLog> findSince(@Param("since") Instant since, @Param("limit") int limit);
}
