package com.example.foreverhome.moderation.repository.admin;

import com.example.foreverhome.moderation.domain.admin.AccountStatus;
import com.example.foreverhome.moderation.domain.admin.User;
import com.example.foreverhome.moderation.domain.admin.UserRole;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends CrudRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query("SELECT * FROM app_users WHERE role = :role AND status = :status")
    List<User> findByRoleAndStatus(@Param("role") UserRole role, @Param("status") AccountStatus status);

    @Query("SELECT COUNT(*) FROM app_users WHERE role = :role")
    long countByRole(@Param("role") UserRole role);

    @Query("SELECT COUNT(*) FROM app_users WHERE status = :status")
    long countByStatus(@Param("status") AccountStatus status);

    @Query("SELECT COUNT(*) FROM app_users WHERE role = :role AND status = :status")
    long countByRoleAndStatus(@Param("role") UserRole role, @Param("status") AccountStatus status);

    @Query("SELECT * FROM app_users ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> findAllPaged(@Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM app_users WHERE role = :role ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> findByRolePaged(@Param("role") UserRole role, @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM app_users WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> findByStatusPaged(@Param("status") AccountStatus status, @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM app_users WHERE role = :role AND status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> findByRoleAndStatusPaged(@Param("role") UserRole role, @Param("status") AccountStatus status,
                                        @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM app_users WHERE LOWER(name) LIKE LOWER(:pattern) OR LOWER(email) LIKE LOWER(:pattern) ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> searchByNameOrEmail(@Param("pattern") String pattern, @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT COUNT(*) FROM app_users WHERE LOWER(name) LIKE LOWER(:pattern) OR LOWER(email) LIKE LOWER(:pattern)")
    long countSearchByNameOrEmail(@Param("pattern") String pattern);
}
