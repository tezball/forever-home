package com.example.foreverhome.domain.profile;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Profile for users who register pets for adoption.
 */
@Table("fosters")
public class Foster implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = false;

    @Column("user_id")
    private UUID userId;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("phone")
    private String phone;

    @Embedded.Nullable(prefix = "address_")
    private Address address;

    protected Foster() {
    }

    private Foster(UUID id, UUID userId, String firstName, String lastName, String phone, Address address) {
        this.id = id;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
    }

    public static Foster create(UUID userId, String firstName, String lastName, String phone, Address address) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("firstName cannot be null or blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("lastName cannot be null or blank");
        }
        Foster foster = new Foster(UUID.randomUUID(), userId, firstName.trim(), lastName.trim(), phone, address);
        foster.isNew = true;
        return foster;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPhone() {
        return phone;
    }

    public Address getAddress() {
        return address;
    }

    public void updateProfile(String firstName, String lastName, String phone, Address address) {
        if (firstName != null && !firstName.isBlank()) {
            this.firstName = firstName.trim();
        }
        if (lastName != null && !lastName.isBlank()) {
            this.lastName = lastName.trim();
        }
        this.phone = phone;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Foster foster = (Foster) o;
        return Objects.equals(id, foster.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
