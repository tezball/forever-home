package com.example.foreverhome.moderation.repository.admin;

import com.example.foreverhome.moderation.domain.admin.ImpersonationSession;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImpersonationSessionRepository extends CrudRepository<ImpersonationSession, UUID> {

    @Query("SELECT * FROM impersonation_sessions WHERE token = :token AND ended_at IS NULL AND expires_at > NOW()")
    Optional<ImpersonationSession> findActiveByToken(@Param("token") String token);

    @Query("SELECT * FROM impersonation_sessions WHERE admin_user_id = :adminUserId ORDER BY created_at DESC LIMIT :limit")
    List<ImpersonationSession> findByAdminUserId(@Param("adminUserId") UUID adminUserId, @Param("limit") int limit);

    @Query("SELECT * FROM impersonation_sessions WHERE target_user_id = :targetUserId ORDER BY created_at DESC LIMIT :limit")
    List<ImpersonationSession> findByTargetUserId(@Param("targetUserId") UUID targetUserId, @Param("limit") int limit);

    @Query("SELECT * FROM impersonation_sessions WHERE admin_user_id = :adminUserId AND ended_at IS NULL AND expires_at > NOW()")
    Optional<ImpersonationSession> findActiveByAdminUserId(@Param("adminUserId") UUID adminUserId);

    @Query("SELECT * FROM impersonation_sessions ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<ImpersonationSession> findAllPaged(@Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT COUNT(*) FROM impersonation_sessions WHERE ended_at IS NULL AND expires_at > NOW()")
    long countActive();
}
