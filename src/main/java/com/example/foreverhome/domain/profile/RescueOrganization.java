package com.example.foreverhome.domain.profile;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Profile for rescue organizations that facilitate pet adoptions.
 */
@Table("rescue_organizations")
public class RescueOrganization {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("name")
    private String name;

    @Column("phone")
    private String phone;

    @Column("website")
    private String website;

    @Column("description")
    private String description;

    @Column("logo_url")
    private String logoUrl;

    @Column("contact_name")
    private String contactName;

    @Column("contact_email")
    private String contactEmail;

    @Column("verified")
    private boolean verified;

    @Embedded.Nullable(prefix = "address_")
    private Address address;

    @Embedded.Nullable(prefix = "social_")
    private SocialLinks socialLinks;

    protected RescueOrganization() {
    }

    private RescueOrganization(UUID id, UUID userId, String name, String phone, String website,
                                String description, String logoUrl, String contactName, String contactEmail,
                                boolean verified, Address address, SocialLinks socialLinks) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.website = website;
        this.description = description;
        this.logoUrl = logoUrl;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.verified = verified;
        this.address = address;
        this.socialLinks = socialLinks;
    }

    public static RescueOrganization create(UUID userId, String name, String phone, String website,
                                             String description, String contactName, String contactEmail,
                                             Address address, SocialLinks socialLinks) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        return new RescueOrganization(UUID.randomUUID(), userId, name.trim(), phone, website,
                description, null, contactName, contactEmail, false, address,
                socialLinks != null ? socialLinks : SocialLinks.empty());
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
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

    public String getContactName() {
        return contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public boolean isVerified() {
        return verified;
    }

    public Address getAddress() {
        return address;
    }

    public SocialLinks getSocialLinks() {
        return socialLinks;
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

    public void updateProfile(String name, String phone, String website, String description,
                              String contactName, String contactEmail, Address address, SocialLinks socialLinks) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        this.phone = phone;
        this.website = website;
        this.description = description;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.address = address;
        if (socialLinks != null) {
            this.socialLinks = socialLinks;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RescueOrganization that = (RescueOrganization) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
