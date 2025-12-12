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
 * Profile for users who want to adopt pets.
 */
@Table("adopters")
public class Adopter implements Persistable<UUID> {

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

    @Column("living_situation")
    private String livingSituation;

    @Column("pet_experience")
    private String petExperience;

    @Embedded.Nullable(prefix = "address_")
    private Address address;

    protected Adopter() {
    }

    private Adopter(UUID id, UUID userId, String firstName, String lastName, String phone,
                    String livingSituation, String petExperience, Address address) {
        this.id = id;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.livingSituation = livingSituation;
        this.petExperience = petExperience;
        this.address = address;
    }

    public static Adopter create(UUID userId, String firstName, String lastName, String phone,
                                  String livingSituation, String petExperience, Address address) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("firstName cannot be null or blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("lastName cannot be null or blank");
        }
        Adopter adopter = new Adopter(UUID.randomUUID(), userId, firstName.trim(), lastName.trim(),
                phone, livingSituation, petExperience, address);
        adopter.isNew = true;
        return adopter;
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

    public String getLivingSituation() {
        return livingSituation;
    }

    public String getPetExperience() {
        return petExperience;
    }

    public Address getAddress() {
        return address;
    }

    public void updateProfile(String firstName, String lastName, String phone,
                              String livingSituation, String petExperience, Address address) {
        if (firstName != null && !firstName.isBlank()) {
            this.firstName = firstName.trim();
        }
        if (lastName != null && !lastName.isBlank()) {
            this.lastName = lastName.trim();
        }
        this.phone = phone;
        this.livingSituation = livingSituation;
        this.petExperience = petExperience;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Adopter adopter = (Adopter) o;
        return Objects.equals(id, adopter.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
