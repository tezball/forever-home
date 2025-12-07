package com.example.foreverhome.repository;

import com.example.foreverhome.domain.user.RefreshToken;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM refresh_tokens WHERE user_id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM refresh_tokens WHERE expires_at < :now")
    void deleteExpired(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE refresh_tokens SET revoked = true WHERE user_id = :userId AND revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);
}
