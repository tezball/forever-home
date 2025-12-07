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

    List<User> findByRole(UserRole role);

    List<User> findByStatus(AccountStatus status);

    @Query("SELECT * FROM users WHERE role = :role AND status = :status")
    List<User> findByRoleAndStatus(@Param("role") UserRole role, @Param("status") AccountStatus status);

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    long countByRole(@Param("role") UserRole role);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);
}
