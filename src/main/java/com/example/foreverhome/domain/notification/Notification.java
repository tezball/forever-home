package com.example.foreverhome.domain.notification;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * System message to a user.
 */
@Table("notifications")
public class Notification {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("type")
    private NotificationType type;

    @Column("title")
    private String title;

    @Column("message")
    private String message;

    @Column("link")
    private String link;

    @Column("read")
    private boolean read;

    @Column("created_at")
    private Instant createdAt;

    protected Notification() {
    }

    private Notification(UUID id, UUID userId, NotificationType type, String title,
                         String message, String link, boolean read, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.read = read;
        this.createdAt = createdAt;
    }

    public static Notification create(UUID userId, NotificationType type, String title, String message) {
        return create(userId, type, title, message, null);
    }

    public static Notification create(UUID userId, NotificationType type, String title,
                                        String message, String link) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title cannot be null or blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message cannot be null or blank");
        }
        return new Notification(UUID.randomUUID(), userId, type, title, message, link, false, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }

    public void markAsRead() {
        this.read = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
