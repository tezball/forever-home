package com.example.foreverhome.domain.pet;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Photos of pets to help adopters connect emotionally.
 * Stores S3 keys rather than full URLs for environment independence.
 */
@Table("pet_images")
public class PetImage implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("pet_id")
    private UUID petId;

    @Column("s3_key")
    private String s3Key;

    @Column("is_primary")
    private boolean primary;

    @Column("display_order")
    private int displayOrder;

    @Column("uploaded_at")
    private Instant uploadedAt;

    @Transient
    private boolean isNew = false;

    protected PetImage() {
    }

    private PetImage(UUID id, UUID petId, String s3Key, boolean primary, int displayOrder, Instant uploadedAt) {
        this.id = id;
        this.petId = petId;
        this.s3Key = s3Key;
        this.primary = primary;
        this.displayOrder = displayOrder;
        this.uploadedAt = uploadedAt;
    }

    public static PetImage create(UUID petId, String s3Key, boolean primary, int displayOrder) {
        if (petId == null) {
            throw new IllegalArgumentException("petId cannot be null");
        }
        if (s3Key == null || s3Key.isBlank()) {
            throw new IllegalArgumentException("s3Key cannot be null or blank");
        }
        PetImage image = new PetImage(UUID.randomUUID(), petId, s3Key, primary, displayOrder, Instant.now());
        image.isNew = true;
        return image;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPetId() {
        return petId;
    }

    public String getS3Key() {
        return s3Key;
    }

    public boolean isPrimary() {
        return primary;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PetImage petImage = (PetImage) o;
        return Objects.equals(id, petImage.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
