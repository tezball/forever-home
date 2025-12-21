package com.example.foreverhome.controller;

import com.example.foreverhome.domain.moderation.AuditLog;
import com.example.foreverhome.domain.moderation.ContentFlag;
import com.example.foreverhome.domain.moderation.ContentFlag.ContentType;
import com.example.foreverhome.domain.moderation.ContentFlag.FlagReason;
import com.example.foreverhome.domain.moderation.ContentFlag.FlagStatus;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.user.AccountStatus;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.logging.UserJourneyLogger;
import com.example.foreverhome.repository.AdopterRepository;
import com.example.foreverhome.repository.AdoptionRepository;
import com.example.foreverhome.repository.FosterRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.RefreshTokenRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.UserRepository;
import com.example.foreverhome.repository.VetRepository;
import com.example.foreverhome.service.EmailService;
import com.example.foreverhome.service.ModerationService;
import com.example.foreverhome.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final AdoptionRepository adoptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ModerationService moderationService;
    private final NotificationService notificationService;
    private final UserJourneyLogger journeyLogger;
    private final FosterRepository fosterRepository;
    private final AdopterRepository adopterRepository;
    private final VetRepository vetRepository;
    private final RescueOrganizationRepository rescueOrgRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminController(UserRepository userRepository, PetRepository petRepository,
                          AdoptionRepository adoptionRepository, PasswordEncoder passwordEncoder,
                          EmailService emailService, ModerationService moderationService,
                          NotificationService notificationService,
                          UserJourneyLogger journeyLogger,
                          FosterRepository fosterRepository,
                          AdopterRepository adopterRepository,
                          VetRepository vetRepository,
                          RescueOrganizationRepository rescueOrgRepository,
                          RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.adoptionRepository = adoptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
        this.journeyLogger = journeyLogger;
        this.fosterRepository = fosterRepository;
        this.adopterRepository = adopterRepository;
        this.vetRepository = vetRepository;
        this.rescueOrgRepository = rescueOrgRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        long totalUsers = userRepository.count();
        long totalPets = petRepository.count();
        long totalAdoptions = adoptionRepository.countAll();

        // Count pending approvals (RESCUE_ORGs with PENDING status - vets are approved by rescues)
        List<User> pendingRescues = userRepository.findByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING);
        long pendingApprovals = pendingRescues.size();

        // User distribution by role
        long adopters = userRepository.countByRole(UserRole.ADOPTER);
        long fosters = userRepository.countByRole(UserRole.FOSTER);
        long rescueOrgs = userRepository.countByRole(UserRole.RESCUE_ORG);
        long vets = userRepository.countByRole(UserRole.VET);
        long admins = userRepository.countByRole(UserRole.ADMIN);

        // Pet statistics by status
        long petsAvailable = petRepository.countByStatus(PetStatus.AVAILABLE);
        long petsPendingRescue = petRepository.countByStatus(PetStatus.PENDING_RESCUE);
        long petsPendingVet = petRepository.countByStatus(PetStatus.PENDING_VET);
        long petsInProgress = petRepository.countByStatus(PetStatus.IN_PROGRESS);
        long petsAdopted = petRepository.countByStatus(PetStatus.ADOPTED);
        long petsDraft = petRepository.countByStatus(PetStatus.DRAFT);
        long petsOnHold = petRepository.countByStatus(PetStatus.ON_HOLD);
        long petsWithdrawn = petRepository.countByStatus(PetStatus.WITHDRAWN);

        var response = new AnalyticsResponse(
                totalUsers,
                totalPets,
                totalAdoptions,
                pendingApprovals,
                new UserDistribution(adopters, fosters, rescueOrgs, vets, admins),
                new PetStatistics(petsAvailable, petsPendingRescue, petsPendingVet, petsInProgress, petsAdopted, petsDraft, petsOnHold, petsWithdrawn)
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/approvals")
    public ResponseEntity<List<ApprovalResponse>> getPendingApprovals() {
        // Only rescue organizations are approved by admins - vets are approved by rescue centers
        List<User> pendingRescues = userRepository.findByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING);

        List<ApprovalResponse> approvals = new java.util.ArrayList<>();

        for (User rescue : pendingRescues) {
            approvals.add(new ApprovalResponse(
                    rescue.getId().toString(),
                    "RESCUE_ORG",
                    rescue.getName() != null ? rescue.getName() : rescue.getEmail(),
                    rescue.getEmail(),
                    rescue.getCreatedAt().toString()
            ));
        }

        return ResponseEntity.ok(approvals);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        List<UserResponse> users = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(u -> new UserResponse(
                        u.getId().toString(),
                        u.getName() != null ? u.getName() : u.getEmail(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.getStatus().name(),
                        u.isProfileComplete()
                ))
                .toList();

        return ResponseEntity.ok(users);
    }

    @PutMapping("/approvals/{type}/{id}/approve")
    public ResponseEntity<Void> approveUser(@PathVariable String type, @PathVariable String id) {
        var userId = java.util.UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.activate();
        userRepository.save(user);

        journeyLogger.logAdminAction(UserJourneyLogger.ACTION_ADMIN_APPROVE_RESCUE, "USER", userId,
                "Approved " + type + " account: " + user.getEmail());

        // Notify rescue org that they've been verified
        if (user.getRole() == UserRole.RESCUE_ORG) {
            notificationService.notifyRescueOrgVerified(userId);
        }

        return ResponseEntity.ok().build();
    }

    @PutMapping("/approvals/{type}/{id}/reject")
    public ResponseEntity<Void> rejectUser(
            @PathVariable String type,
            @PathVariable String id,
            @RequestBody(required = false) RejectRequest request) {
        var userId = java.util.UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.suspend();
        userRepository.save(user);

        String reason = request != null ? request.reason() : null;
        journeyLogger.logAdminAction(UserJourneyLogger.ACTION_ADMIN_REJECT_RESCUE, "USER", userId,
                "Rejected " + type + " account: " + user.getEmail() + (reason != null ? " - Reason: " + reason : ""));

        // Notify rescue org that they've been rejected
        if (user.getRole() == UserRole.RESCUE_ORG) {
            notificationService.notifyRescueOrgRejected(userId, reason);
        }

        return ResponseEntity.ok().build();
    }

    record RejectRequest(String reason) {}

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable String id) {
        var userId = java.util.UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.suspend();
        userRepository.save(user);

        journeyLogger.logAdminAction(UserJourneyLogger.ACTION_ADMIN_SUSPEND_USER, "USER", userId,
                "Suspended user: " + user.getEmail());

        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable String id) {
        var userId = java.util.UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.reactivate();
        userRepository.save(user);

        journeyLogger.logAdminAction(UserJourneyLogger.ACTION_ADMIN_REACTIVATE_USER, "USER", userId,
                "Reactivated user: " + user.getEmail());

        return ResponseEntity.ok().build();
    }

    /**
     * Permanently delete a user and all their associated data.
     * This is a destructive operation and cannot be undone.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        var userId = java.util.UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String email = user.getEmail();
        UserRole role = user.getRole();

        // Prevent deleting admin accounts (safety measure)
        if (role == UserRole.ADMIN) {
            throw new IllegalStateException("Cannot delete admin accounts");
        }

        // Delete associated profile data based on role
        switch (role) {
            case FOSTER -> fosterRepository.findByUserId(userId).ifPresent(fosterRepository::delete);
            case ADOPTER -> adopterRepository.findByUserId(userId).ifPresent(adopterRepository::delete);
            case VET -> vetRepository.findByUserId(userId).ifPresent(vetRepository::delete);
            case RESCUE_ORG -> rescueOrgRepository.findByUserId(userId).ifPresent(rescueOrgRepository::delete);
            default -> { /* No profile to delete */ }
        }

        // Delete refresh tokens
        refreshTokenRepository.deleteByUserId(userId);

        // Delete the user
        userRepository.delete(user);

        journeyLogger.logAdminAction(UserJourneyLogger.ACTION_ADMIN_DELETE_USER, "USER", userId,
                "Permanently deleted user: " + email + " (role: " + role + ")");

        return ResponseEntity.ok().build();
    }

    /**
     * Search users with filtering and pagination.
     */
    @GetMapping("/users/search")
    public ResponseEntity<UserSearchResponse> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        int limit = Math.min(size, 100);
        int offset = (page - 1) * limit;

        List<User> users;
        long totalCount;

        // Handle search with query
        if (query != null && !query.isBlank()) {
            String searchPattern = "%" + query.trim() + "%";
            users = userRepository.searchByNameOrEmail(searchPattern, limit, offset);
            totalCount = userRepository.countSearchByNameOrEmail(searchPattern);
        }
        // Handle role and status filtering
        else if (role != null && status != null) {
            UserRole userRole = UserRole.valueOf(role);
            AccountStatus accountStatus = AccountStatus.valueOf(status);
            users = userRepository.findByRoleAndStatusPaged(userRole, accountStatus, limit, offset);
            totalCount = userRepository.countByRoleAndStatus(userRole, accountStatus);
        }
        else if (role != null) {
            UserRole userRole = UserRole.valueOf(role);
            users = userRepository.findByRolePaged(userRole, limit, offset);
            totalCount = userRepository.countByRole(userRole);
        }
        else if (status != null) {
            AccountStatus accountStatus = AccountStatus.valueOf(status);
            users = userRepository.findByStatusPaged(accountStatus, limit, offset);
            totalCount = userRepository.countByStatus(accountStatus);
        }
        else {
            users = userRepository.findAllPaged(limit, offset);
            totalCount = userRepository.count();
        }

        List<UserResponse> userResponses = users.stream()
                .map(u -> new UserResponse(
                        u.getId().toString(),
                        u.getName() != null ? u.getName() : u.getEmail(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.getStatus().name(),
                        u.isProfileComplete()
                ))
                .toList();

        int totalPages = (int) Math.ceil((double) totalCount / limit);

        return ResponseEntity.ok(new UserSearchResponse(
                userResponses,
                page,
                limit,
                totalCount,
                totalPages
        ));
    }

    /**
     * Reset a user's password and send them a new temporary password via email.
     */
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetUserPassword(@PathVariable String id) {
        var userId = UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Generate a random temporary password
        String tempPassword = generateTemporaryPassword();

        // Hash and save the new password
        user.updatePasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // Send the temporary password via email
        emailService.sendPasswordResetByAdmin(user.getEmail(), tempPassword);

        journeyLogger.logAdminAction(UserJourneyLogger.ACTION_ADMIN_RESET_PASSWORD, "USER", userId,
                "Password reset email sent to: " + user.getEmail());

        return ResponseEntity.ok(new ResetPasswordResponse("Password reset email sent to " + user.getEmail()));
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ==================== MODERATION ENDPOINTS ====================

    @GetMapping("/flags")
    public ResponseEntity<FlagSearchResponse> getFlags(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        FlagStatus flagStatus = status != null ? FlagStatus.valueOf(status) : null;
        List<ContentFlag> flags = moderationService.getFlags(flagStatus, page, size);
        long totalCount = moderationService.countFlags(flagStatus);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        List<FlagResponse> flagResponses = flags.stream()
                .map(f -> new FlagResponse(
                        f.getId().toString(),
                        f.getContentType().name(),
                        f.getContentId().toString(),
                        f.getReporterId() != null ? f.getReporterId().toString() : null,
                        f.getReason().name(),
                        f.getDescription(),
                        f.getStatus().name(),
                        f.getReviewedBy() != null ? f.getReviewedBy().toString() : null,
                        f.getReviewedAt() != null ? f.getReviewedAt().toString() : null,
                        f.getResolutionNotes(),
                        f.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(new FlagSearchResponse(flagResponses, page, size, totalCount, totalPages));
    }

    @PutMapping("/flags/{id}/approve")
    public ResponseEntity<FlagResponse> approveFlag(
            @PathVariable String id,
            @RequestBody FlagResolveRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.example.foreverhome.security.UserPrincipal principal) {

        UUID flagId = UUID.fromString(id);
        ContentFlag flag = moderationService.approveFlag(flagId, principal.userId(), request.notes());

        return ResponseEntity.ok(toFlagResponse(flag));
    }

    @PutMapping("/flags/{id}/dismiss")
    public ResponseEntity<FlagResponse> dismissFlag(
            @PathVariable String id,
            @RequestBody FlagResolveRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.example.foreverhome.security.UserPrincipal principal) {

        UUID flagId = UUID.fromString(id);
        ContentFlag flag = moderationService.dismissFlag(flagId, principal.userId(), request.notes());

        return ResponseEntity.ok(toFlagResponse(flag));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<AuditLog> logs = moderationService.getAuditLogs(page, size);

        List<AuditLogResponse> responses = logs.stream()
                .map(l -> new AuditLogResponse(
                        l.getId().toString(),
                        l.getAction().name(),
                        l.getEntityType(),
                        l.getEntityId() != null ? l.getEntityId().toString() : null,
                        l.getActorId() != null ? l.getActorId().toString() : null,
                        l.getDetails(),
                        l.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Reset all pet moderation statuses to PENDING.
     * Used to re-queue all pets for AI moderation after system changes.
     */
    @PostMapping("/moderation/reset")
    public ResponseEntity<ModerationResetResponse> resetModerationStatuses(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.example.foreverhome.security.UserPrincipal principal) {

        int count = petRepository.resetAllModerationStatuses();

        journeyLogger.logAdminAction("MODERATION_RESET", "PET", principal.userId(),
                "Reset all pet moderation statuses to PENDING. " + count + " pets affected");

        return ResponseEntity.ok(new ModerationResetResponse(count, "All pet moderation statuses reset to PENDING"));
    }

    private FlagResponse toFlagResponse(ContentFlag flag) {
        return new FlagResponse(
                flag.getId().toString(),
                flag.getContentType().name(),
                flag.getContentId().toString(),
                flag.getReporterId() != null ? flag.getReporterId().toString() : null,
                flag.getReason().name(),
                flag.getDescription(),
                flag.getStatus().name(),
                flag.getReviewedBy() != null ? flag.getReviewedBy().toString() : null,
                flag.getReviewedAt() != null ? flag.getReviewedAt().toString() : null,
                flag.getResolutionNotes(),
                flag.getCreatedAt().toString()
        );
    }

    // Response DTOs
    record AnalyticsResponse(
            long totalUsers,
            long totalPets,
            long totalAdoptions,
            long pendingApprovals,
            UserDistribution userDistribution,
            PetStatistics petStatistics
    ) {}

    record UserDistribution(
            long adopters,
            long fosters,
            long rescueOrgs,
            long vets,
            long admins
    ) {}

    record PetStatistics(
            long available,
            long pendingRescue,
            long pendingVet,
            long inProgress,
            long adopted,
            long draft,
            long onHold,
            long withdrawn
    ) {}

    record ApprovalResponse(
            String id,
            String type,
            String name,
            String email,
            String submittedAt
    ) {}

    record UserResponse(
            String id,
            String name,
            String email,
            String role,
            String status,
            boolean profileComplete
    ) {}

    record UserSearchResponse(
            List<UserResponse> users,
            int page,
            int size,
            long totalCount,
            int totalPages
    ) {}

    record ResetPasswordResponse(String message) {}

    record FlagResponse(
            String id,
            String contentType,
            String contentId,
            String reporterId,
            String reason,
            String description,
            String status,
            String reviewedBy,
            String reviewedAt,
            String resolutionNotes,
            String createdAt
    ) {}

    record FlagSearchResponse(
            List<FlagResponse> flags,
            int page,
            int size,
            long totalCount,
            int totalPages
    ) {}

    record FlagResolveRequest(String notes) {}

    record AuditLogResponse(
            String id,
            String action,
            String entityType,
            String entityId,
            String actorId,
            String details,
            String createdAt
    ) {}

    record ModerationResetResponse(int petsAffected, String message) {}
}
