package com.example.foreverhome.domain.verification;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records which rescue organizations have approved which vets.
 * A vet must be approved by a rescue organization before they can
 * verify pets belonging to that organization.
 */
@Table("vet_approvals")
public class VetApproval {

    @Id
    private UUID id;

    @Column("vet_id")
    private UUID vetId;

    @Column("rescue_org_id")
    private UUID rescueOrgId;

    @Column("approved_at")
    private Instant approvedAt;

    @Column("approved_by")
    private UUID approvedBy;

    protected VetApproval() {
    }

    private VetApproval(UUID id, UUID vetId, UUID rescueOrgId, Instant approvedAt, UUID approvedBy) {
        this.id = id;
        this.vetId = vetId;
        this.rescueOrgId = rescueOrgId;
        this.approvedAt = approvedAt;
        this.approvedBy = approvedBy;
    }

    /**
     * Creates a new vet approval record.
     *
     * @param vetId       The ID of the vet being approved
     * @param rescueOrgId The ID of the rescue organization approving the vet
     * @param approvedBy  The ID of the user (rescue org representative) who approved
     * @return A new VetApproval instance
     */
    public static VetApproval create(UUID vetId, UUID rescueOrgId, UUID approvedBy) {
        if (vetId == null) {
            throw new IllegalArgumentException("vetId cannot be null");
        }
        if (rescueOrgId == null) {
            throw new IllegalArgumentException("rescueOrgId cannot be null");
        }
        if (approvedBy == null) {
            throw new IllegalArgumentException("approvedBy cannot be null");
        }
        return new VetApproval(UUID.randomUUID(), vetId, rescueOrgId, Instant.now(), approvedBy);
    }

    public UUID getId() {
        return id;
    }

    public UUID getVetId() {
        return vetId;
    }

    public UUID getRescueOrgId() {
        return rescueOrgId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VetApproval that = (VetApproval) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
