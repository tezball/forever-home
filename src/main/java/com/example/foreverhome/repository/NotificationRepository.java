package com.example.foreverhome.repository;

import com.example.foreverhome.domain.notification.Notification;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends CrudRepository<Notification, UUID> {

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND read = false ORDER BY created_at DESC")
    List<Notification> findUnreadByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND read = false")
    int countUnreadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE notifications SET read = true WHERE user_id = :userId")
    void markAllAsReadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM notifications WHERE user_id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
