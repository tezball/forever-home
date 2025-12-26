package com.example.foreverhome.moderation.repository.admin;

import com.example.foreverhome.moderation.domain.admin.BlockedIp;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockedIpRepository extends CrudRepository<BlockedIp, UUID> {

    @Query("SELECT * FROM blocked_ips WHERE active = true ORDER BY blocked_at DESC")
    List<BlockedIp> findAllActive();

    @Query("SELECT * FROM blocked_ips ORDER BY blocked_at DESC LIMIT :limit OFFSET :offset")
    List<BlockedIp> findAllPaged(@Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT * FROM blocked_ips WHERE ip_address = :ipAddress AND active = true")
    Optional<BlockedIp> findActiveByIpAddress(@Param("ipAddress") String ipAddress);

    @Query("SELECT * FROM blocked_ips WHERE active = true AND (ip_address = :ip OR (:ip LIKE cidr_range || '%'))")
    List<BlockedIp> findActiveMatchingIp(@Param("ip") String ip);

    @Query("SELECT COUNT(*) FROM blocked_ips WHERE active = true")
    long countActive();

    @Query("SELECT COUNT(*) FROM blocked_ips WHERE active = true AND permanent = true")
    long countPermanent();

    @Query("SELECT COUNT(*) FROM blocked_ips WHERE active = true AND permanent = false AND expires_at > NOW()")
    long countTemporary();

    @Query("SELECT * FROM blocked_ips WHERE active = true AND permanent = false AND expires_at <= NOW()")
    List<BlockedIp> findExpired();
}
