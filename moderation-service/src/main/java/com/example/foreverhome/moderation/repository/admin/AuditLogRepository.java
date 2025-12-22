package com.example.foreverhome.moderation.repository.admin;

import com.example.foreverhome.moderation.domain.admin.AuditLog;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends CrudRepository<AuditLog, UUID> {

    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<AuditLog> findAllPaged(@Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM audit_logs WHERE actor_id = :actorId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<AuditLog> findByActorIdPaged(@Param("actorId") UUID actorId, @Param("limit") int limit, @Param("offset") int offset);
}
