package com.example.foreverhome.repository;

import com.example.foreverhome.domain.user.AccountStatus;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
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

    boolean existsByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByGoogleId(String googleId);

    List<User> findByRole(UserRole role);

    List<User> findByStatus(AccountStatus status);

    @Query("SELECT * FROM app_users WHERE role = :role AND status = :status")
    List<User> findByRoleAndStatus(@Param("role") UserRole role, @Param("status") AccountStatus status);

    @Query("SELECT COUNT(*) FROM app_users WHERE role = :role")
    long countByRole(@Param("role") UserRole role);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    @Query("SELECT * FROM app_users WHERE (LOWER(name) LIKE LOWER(:query) OR LOWER(email) LIKE LOWER(:query)) ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> searchByNameOrEmail(@Param("query") String query, @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT COUNT(*) FROM app_users WHERE (LOWER(name) LIKE LOWER(:query) OR LOWER(email) LIKE LOWER(:query))")
    long countSearchByNameOrEmail(@Param("query") String query);

    @Query("SELECT * FROM app_users WHERE role = :role ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> findByRolePaged(@Param("role") UserRole role, @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM app_users WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> findByStatusPaged(@Param("status") AccountStatus status, @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM app_users WHERE role = :role AND status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> findByRoleAndStatusPaged(@Param("role") UserRole role, @Param("status") AccountStatus status, @Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT COUNT(*) FROM app_users WHERE role = :role AND status = :status")
    long countByRoleAndStatus(@Param("role") UserRole role, @Param("status") AccountStatus status);

    @Query("SELECT COUNT(*) FROM app_users WHERE status = :status")
    long countByStatus(@Param("status") AccountStatus status);

    @Query("SELECT * FROM app_users ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<User> findAllPaged(@Param("limit") int limit, @Param("offset") int offset);
}
