package com.example.foreverhome.domain.profile;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Minimal profile for platform administrators.
 */
@Table("admins")
public class Admin {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    protected Admin() {
    }

    private Admin(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
    }

    public static Admin create(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        return new Admin(UUID.randomUUID(), userId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Admin admin = (Admin) o;
        return Objects.equals(id, admin.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
