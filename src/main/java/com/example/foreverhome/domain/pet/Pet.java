package com.example.foreverhome.domain.pet;

import com.example.foreverhome.exception.InvalidStateTransitionException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Central entity representing an animal seeking a forever home.
 */
@Table("pets")
public class Pet implements Persistable<UUID> {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @Id
    private UUID id;

    @Column("name")
    private String name;

    @Column("species")
    private Species species;

    @Column("breed")
    private Breed breed;

    @Column("age")
    private int age;

    @Column("age_unit")
    private AgeUnit ageUnit;

    @Column("sex")
    private PetSex sex;

    @Column("size")
    private PetSize size;

    @Column("description")
    private String description;

    @Column("microchip_id")
    private String microchipId;

    @Column("status")
    private PetStatus status;

    @Column("foster_id")
    private UUID fosterId;

    @Column("rescue_org_id")
    private UUID rescueOrgId;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("health_notes")
    private String healthNotes;

    @Column("moderation_status")
    private ModerationStatus moderationStatus;

    @Column("moderated_at")
    private Instant moderatedAt;

    @Column("moderation_notes")
    private String moderationNotes;

    @Transient
    private boolean isNew = false;

    protected Pet() {
    }

    private Pet(UUID id, String name, Species species, Breed breed, int age, AgeUnit ageUnit,
                PetSex sex, PetSize size, String description, String microchipId, PetStatus status,
                UUID fosterId, UUID rescueOrgId, Instant createdAt, Instant updatedAt, String healthNotes,
                ModerationStatus moderationStatus, Instant moderatedAt, String moderationNotes) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.ageUnit = ageUnit;
        this.sex = sex;
        this.size = size;
        this.description = description;
        this.microchipId = microchipId;
        this.status = status;
        this.fosterId = fosterId;
        this.rescueOrgId = rescueOrgId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.healthNotes = healthNotes;
        this.moderationStatus = moderationStatus;
        this.moderatedAt = moderatedAt;
        this.moderationNotes = moderationNotes;
    }

    public static Pet create(String name, Species species, Breed breed, int age, AgeUnit ageUnit,
                              PetSex sex, PetSize size, String description, String microchipId, UUID fosterId) {
        return create(fosterId, name, species, breed, age, ageUnit, sex, size, microchipId, description, null);
    }

    public static Pet create(UUID fosterId, String name, Species species, Breed breed, int age, AgeUnit ageUnit,
                              PetSex sex, PetSize size, String microchipId, String description, String healthNotes) {
        validateRequired(name, "name");
        validateRequired(species, "species");
        validateRequired(ageUnit, "ageUnit");
        validateRequired(sex, "sex");
        validateRequired(size, "size");
        validateRequired(microchipId, "microchipId");
        validateRequired(fosterId, "fosterId");

        if (age < 0) {
            throw new IllegalArgumentException("age must be non-negative");
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        Instant now = Instant.now();
        Pet pet = new Pet(UUID.randomUUID(), name.trim(), species, breed, age, ageUnit,
                sex, size, description, microchipId.trim(), PetStatus.DRAFT,
                fosterId, null, now, now, healthNotes,
                ModerationStatus.PENDING, null, null);
        pet.isNew = true;
        return pet;
    }

    /**
     * Creates a pet directly owned by a rescue organization.
     * Rescue-created pets start in PENDING_VET status, skipping the foster submission workflow.
     *
     * @param rescueOrgId The ID of the rescue organization creating the pet
     * @param name Pet name
     * @param species Pet species
     * @param breed Pet breed (nullable)
     * @param age Pet age
     * @param ageUnit Unit for age (months/years)
     * @param sex Pet sex
     * @param size Pet size
     * @param microchipId Microchip ID (required, unique)
     * @param description Pet description (nullable)
     * @param healthNotes Health notes (nullable)
     * @return A new Pet entity in PENDING_VET status
     */
    public static Pet createForRescue(UUID rescueOrgId, String name, Species species, Breed breed,
                                       int age, AgeUnit ageUnit, PetSex sex, PetSize size,
                                       String microchipId, String description, String healthNotes) {
        validateRequired(name, "name");
        validateRequired(species, "species");
        validateRequired(ageUnit, "ageUnit");
        validateRequired(sex, "sex");
        validateRequired(size, "size");
        validateRequired(microchipId, "microchipId");
        validateRequired(rescueOrgId, "rescueOrgId");

        if (age < 0) {
            throw new IllegalArgumentException("age must be non-negative");
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        Instant now = Instant.now();
        Pet pet = new Pet(UUID.randomUUID(), name.trim(), species, breed, age, ageUnit,
                sex, size, description, microchipId.trim(), PetStatus.PENDING_VET,
                null,       // fosterId is null for rescue-created pets
                rescueOrgId, // rescueOrgId is set at creation
                now, now, healthNotes,
                ModerationStatus.PENDING, null, null);
        pet.isNew = true;
        return pet;
    }

    private static void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (value instanceof String s && s.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
    }

    // Persistable implementation
    @Override
    public boolean isNew() {
        return isNew;
    }

    // Getters
    @Override
    public UUID getId() { return id; }
    public String getName() { return name; }
    public Species getSpecies() { return species; }
    public Breed getBreed() { return breed; }
    public int getAge() { return age; }
    public AgeUnit getAgeUnit() { return ageUnit; }
    public PetSex getSex() { return sex; }
    public PetSize getSize() { return size; }
    public String getDescription() { return description; }
    public String getMicrochipId() { return microchipId; }
    public PetStatus getStatus() { return status; }
    public UUID getFosterId() { return fosterId; }
    public UUID getRescueOrgId() { return rescueOrgId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getHealthNotes() { return healthNotes; }
    public ModerationStatus getModerationStatus() { return moderationStatus; }
    public Instant getModeratedAt() { return moderatedAt; }
    public String getModerationNotes() { return moderationNotes; }

    public String getFormattedAge() {
        return ageUnit.formatAge(age);
    }

    /**
     * Returns true if this pet was created directly by a rescue organization
     * (i.e., has no associated foster).
     */
    public boolean isRescueOwned() {
        return fosterId == null;
    }

    /**
     * Returns true if the moderation status is blocking the pet from becoming publicly visible.
     * Only APPROVED status allows visibility.
     */
    public boolean isModerationBlocking() {
        return moderationStatus == null || moderationStatus.isBlocking();
    }

    /**
     * Updates the moderation status of this pet.
     *
     * @param status the new moderation status
     * @param notes optional notes explaining the status (e.g., reason for flagging)
     */
    public void markModerated(ModerationStatus status, String notes) {
        validateRequired(status, "status");
        this.moderationStatus = status;
        this.moderatedAt = Instant.now();
        this.moderationNotes = notes;
        this.updatedAt = Instant.now();
    }

    public boolean isPubliclyVisible() {
        return status.isPubliclyVisible();
    }

    public boolean isEditable() {
        return status.isEditable();
    }

    public boolean canTransitionTo(PetStatus newStatus) {
        return status.canTransitionTo(newStatus);
    }

    // Status transitions
    public void submitForReview(UUID rescueOrgId) {
        validateRequired(rescueOrgId, "rescueOrgId");
        transitionTo(PetStatus.PENDING_RESCUE);
        this.rescueOrgId = rescueOrgId;
    }

    /**
     * Change the rescue organization for a pet in PENDING_RESCUE status.
     * Only allowed when the pet is awaiting rescue acceptance.
     *
     * @param newRescueOrgId The new rescue organization ID
     * @throws IllegalStateException if pet is not in PENDING_RESCUE status
     */
    public void changeRescueOrg(UUID newRescueOrgId) {
        validateRequired(newRescueOrgId, "newRescueOrgId");
        if (status != PetStatus.PENDING_RESCUE) {
            throw new IllegalStateException(
                    "Can only change rescue organization when status is PENDING_RESCUE, current status: " + status);
        }
        this.rescueOrgId = newRescueOrgId;
        this.updatedAt = Instant.now();
    }

    public void acceptByRescue() {
        transitionTo(PetStatus.PENDING_VET);
    }

    public void declineByRescue() {
        transitionTo(PetStatus.DRAFT);
        this.rescueOrgId = null;
    }

    public void signOffByVet() {
        if (isModerationBlocking()) {
            throw new IllegalStateException(
                    "Pet cannot become available: moderation status is " + moderationStatus);
        }
        transitionTo(PetStatus.AVAILABLE);
    }

    public void declineByVet() {
        if (isRescueOwned()) {
            // For rescue-owned pets, stay in PENDING_VET (no foster to return to)
            // Rescue can address vet concerns and resubmit
            this.updatedAt = Instant.now();
        } else {
            transitionTo(PetStatus.PENDING_RESCUE);
        }
    }

    public void approveApplication() {
        transitionTo(PetStatus.IN_PROGRESS);
    }

    public void finalizeAdoption() {
        transitionTo(PetStatus.ADOPTED);
    }

    public void cancelAdoption() {
        transitionTo(PetStatus.AVAILABLE);
    }

    public void withdraw() {
        transitionTo(PetStatus.WITHDRAWN);
    }

    public void putOnHold() {
        transitionTo(PetStatus.ON_HOLD);
    }

    public void removeHold() {
        transitionTo(PetStatus.AVAILABLE);
    }

    private void transitionTo(PetStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition pet from " + status + " to " + newStatus);
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    // Update methods
    public void updateDetails(String name, Breed breed, int age, AgeUnit ageUnit,
                               PetSex sex, PetSize size, String description) {
        if (!isEditable()) {
            throw new IllegalStateException("Pet cannot be edited in status: " + status);
        }
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        this.breed = breed;
        if (age > 0) {
            this.age = age;
        }
        if (ageUnit != null) {
            this.ageUnit = ageUnit;
        }
        if (sex != null) {
            this.sex = sex;
        }
        if (size != null) {
            this.size = size;
        }
        if (description != null && description.length() <= MAX_DESCRIPTION_LENGTH) {
            this.description = description;
        }
        this.updatedAt = Instant.now();
    }

    public void updateName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
            this.updatedAt = Instant.now();
        }
    }

    public void updateDescription(String description) {
        if (description != null && description.length() <= MAX_DESCRIPTION_LENGTH) {
            this.description = description;
            this.updatedAt = Instant.now();
        }
    }

    public void updateHealthNotes(String healthNotes) {
        this.healthNotes = healthNotes;
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pet pet = (Pet) o;
        return Objects.equals(id, pet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
