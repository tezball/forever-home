package com.example.foreverhome.domain.profile;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Profile for veterinary professionals who verify pet health.
 */
@Table("vets")
public class Vet {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("clinic_name")
    private String clinicName;

    @Column("license_number")
    private String licenseNumber;

    @Column("phone")
    private String phone;

    @Column("website")
    private String website;

    @Column("description")
    private String description;

    @Column("logo_url")
    private String logoUrl;

    @Column("verified")
    private boolean verified;

    @Embedded.Nullable(prefix = "address_")
    private Address address;

    protected Vet() {
    }

    private Vet(UUID id, UUID userId, String clinicName, String licenseNumber, String phone,
                String website, String description, String logoUrl, boolean verified, Address address) {
        this.id = id;
        this.userId = userId;
        this.clinicName = clinicName;
        this.licenseNumber = licenseNumber;
        this.phone = phone;
        this.website = website;
        this.description = description;
        this.logoUrl = logoUrl;
        this.verified = verified;
        this.address = address;
    }

    public static Vet create(UUID userId, String clinicName, String licenseNumber, String phone,
                              String website, String description, Address address) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (clinicName == null || clinicName.isBlank()) {
            throw new IllegalArgumentException("clinicName cannot be null or blank");
        }
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new IllegalArgumentException("licenseNumber cannot be null or blank");
        }
        return new Vet(UUID.randomUUID(), userId, clinicName.trim(), licenseNumber.trim(),
                phone, website, description, null, false, address);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getClinicName() {
        return clinicName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getPhone() {
        return phone;
    }

    public String getWebsite() {
        return website;
    }

    public String getDescription() {
        return description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isVerified() {
        return verified;
    }

    public Address getAddress() {
        return address;
    }

    public void verify() {
        this.verified = true;
    }

    public void unverify() {
        this.verified = false;
    }

    public void updateLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void updateProfile(String clinicName, String phone, String website, String description, Address address) {
        if (clinicName != null && !clinicName.isBlank()) {
            this.clinicName = clinicName.trim();
        }
        this.phone = phone;
        this.website = website;
        this.description = description;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vet vet = (Vet) o;
        return Objects.equals(id, vet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
