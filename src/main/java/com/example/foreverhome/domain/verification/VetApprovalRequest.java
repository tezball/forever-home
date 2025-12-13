package com.example.foreverhome.domain.verification;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A request from a vet to be approved by a rescue organization.
 * Vets can request approval from specific rescue organizations,
 * which the organization can then approve or reject.
 */
@Table("vet_approval_requests")
public class VetApprovalRequest implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("vet_id")
    private UUID vetId;

    @Column("rescue_org_id")
    private UUID rescueOrgId;

    @Column("status")
    private VetApprovalRequestStatus status;

    @Column("message")
    private String message;

    @Column("requested_at")
    private Instant requestedAt;

    @Column("reviewed_at")
    private Instant reviewedAt;

    @Column("reviewed_by")
    private UUID reviewedBy;

    @Column("rejection_reason")
    private String rejectionReason;

    @Transient
    private boolean isNew = false;

    protected VetApprovalRequest() {
    }

    private VetApprovalRequest(UUID id, UUID vetId, UUID rescueOrgId, VetApprovalRequestStatus status,
                                String message, Instant requestedAt, Instant reviewedAt,
                                UUID reviewedBy, String rejectionReason, boolean isNew) {
        this.id = id;
        this.vetId = vetId;
        this.rescueOrgId = rescueOrgId;
        this.status = status;
        this.message = message;
        this.requestedAt = requestedAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.rejectionReason = rejectionReason;
        this.isNew = isNew;
    }

    /**
     * Creates a new approval request from a vet to a rescue organization.
     *
     * @param vetId       The ID of the vet requesting approval
     * @param rescueOrgId The ID of the rescue organization
     * @param message     Optional message from the vet explaining their request
     * @return A new VetApprovalRequest in PENDING status
     */
    public static VetApprovalRequest create(UUID vetId, UUID rescueOrgId, String message) {
        if (vetId == null) {
            throw new IllegalArgumentException("vetId cannot be null");
        }
        if (rescueOrgId == null) {
            throw new IllegalArgumentException("rescueOrgId cannot be null");
        }
        return new VetApprovalRequest(
                UUID.randomUUID(),
                vetId,
                rescueOrgId,
                VetApprovalRequestStatus.PENDING,
                message,
                Instant.now(),
                null,
                null,
                null,
                true
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getVetId() {
        return vetId;
    }

    public UUID getRescueOrgId() {
        return rescueOrgId;
    }

    public VetApprovalRequestStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public boolean isPending() {
        return status == VetApprovalRequestStatus.PENDING;
    }

    public boolean isApproved() {
        return status == VetApprovalRequestStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == VetApprovalRequestStatus.REJECTED;
    }

    public boolean canApprove() {
        return status == VetApprovalRequestStatus.PENDING;
    }

    public boolean canReject() {
        return status == VetApprovalRequestStatus.PENDING;
    }

    public boolean canWithdraw() {
        return status == VetApprovalRequestStatus.PENDING;
    }

    /**
     * Approves the request.
     *
     * @param reviewerId The ID of the user approving the request
     */
    public void approve(UUID reviewerId) {
        if (!canApprove()) {
            throw new IllegalStateException("Cannot approve request with status: " + status);
        }
        if (reviewerId == null) {
            throw new IllegalArgumentException("reviewerId cannot be null");
        }
        this.status = VetApprovalRequestStatus.APPROVED;
        this.reviewedAt = Instant.now();
        this.reviewedBy = reviewerId;
    }

    /**
     * Rejects the request with an optional reason.
     *
     * @param reviewerId The ID of the user rejecting the request
     * @param reason     Optional reason for rejection
     */
    public void reject(UUID reviewerId, String reason) {
        if (!canReject()) {
            throw new IllegalStateException("Cannot reject request with status: " + status);
        }
        if (reviewerId == null) {
            throw new IllegalArgumentException("reviewerId cannot be null");
        }
        this.status = VetApprovalRequestStatus.REJECTED;
        this.reviewedAt = Instant.now();
        this.reviewedBy = reviewerId;
        this.rejectionReason = reason;
    }

    /**
     * Withdraws the request. Can only be done by the vet who made the request.
     */
    public void withdraw() {
        if (!canWithdraw()) {
            throw new IllegalStateException("Cannot withdraw request with status: " + status);
        }
        this.status = VetApprovalRequestStatus.WITHDRAWN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VetApprovalRequest that = (VetApprovalRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
