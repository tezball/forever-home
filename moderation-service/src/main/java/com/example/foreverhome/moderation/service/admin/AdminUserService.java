package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.*;
import com.example.foreverhome.moderation.repository.admin.AuditLogRepository;
import com.example.foreverhome.moderation.repository.admin.RescueOrganizationRepository;
import com.example.foreverhome.moderation.repository.admin.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RescueOrganizationRepository rescueOrgRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository,
                            RescueOrganizationRepository rescueOrgRepository,
                            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.rescueOrgRepository = rescueOrgRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public List<User> listUsers(int page, int size) {
        int limit = Math.min(size, 100);
        int offset = (page - 1) * limit;
        return userRepository.findAllPaged(limit, offset);
    }

    public long countUsers() {
        return userRepository.count();
    }

    public UserSearchResult searchUsers(String query, UserRole role, AccountStatus status, int page, int size) {
        int limit = Math.min(size, 100);
        int offset = (page - 1) * limit;

        List<User> users;
        long totalCount;

        if (query != null && !query.isBlank()) {
            String searchPattern = "%" + query.trim() + "%";
            users = userRepository.searchByNameOrEmail(searchPattern, limit, offset);
            totalCount = userRepository.countSearchByNameOrEmail(searchPattern);
        } else if (role != null && status != null) {
            users = userRepository.findByRoleAndStatusPaged(role, status, limit, offset);
            totalCount = userRepository.countByRoleAndStatus(role, status);
        } else if (role != null) {
            users = userRepository.findByRolePaged(role, limit, offset);
            totalCount = userRepository.countByRole(role);
        } else if (status != null) {
            users = userRepository.findByStatusPaged(status, limit, offset);
            totalCount = userRepository.countByStatus(status);
        } else {
            users = userRepository.findAllPaged(limit, offset);
            totalCount = userRepository.count();
        }

        int totalPages = (int) Math.ceil((double) totalCount / limit);

        return new UserSearchResult(users, page, limit, totalCount, totalPages);
    }

    public Optional<User> getUser(UUID userId) {
        return userRepository.findById(userId);
    }

    public List<User> getPendingApprovals() {
        return userRepository.findByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING);
    }

    @Transactional
    public void approveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.activate();
        userRepository.save(user);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.RESCUE_ORG_APPROVED,
                "USER",
                userId,
                null,
                "Approved rescue organization: " + user.getEmail()
        ));
    }

    @Transactional
    public void rejectUser(UUID userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.suspend();
        userRepository.save(user);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.RESCUE_ORG_REJECTED,
                "USER",
                userId,
                null,
                "Rejected rescue organization: " + user.getEmail() + (reason != null ? " - Reason: " + reason : "")
        ));
    }

    @Transactional
    public void suspendUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.suspend();
        userRepository.save(user);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.USER_SUSPENDED,
                "USER",
                userId,
                null,
                "Suspended user: " + user.getEmail()
        ));
    }

    @Transactional
    public void reactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.reactivate();
        userRepository.save(user);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.USER_REACTIVATED,
                "USER",
                userId,
                null,
                "Reactivated user: " + user.getEmail()
        ));
    }

    @Transactional
    public String resetPassword(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String tempPassword = generateTemporaryPassword();
        user.updatePasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.USER_PASSWORD_RESET,
                "USER",
                userId,
                null,
                "Password reset for user: " + user.getEmail()
        ));

        return tempPassword;
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalStateException("Cannot delete admin accounts");
        }

        String email = user.getEmail();
        UserRole role = user.getRole();

        // Delete associated rescue org profile if exists
        if (role == UserRole.RESCUE_ORG) {
            rescueOrgRepository.findByUserId(userId).ifPresent(rescueOrgRepository::delete);
        }

        userRepository.delete(user);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.USER_DELETED,
                "USER",
                userId,
                null,
                "Permanently deleted user: " + email + " (role: " + role + ")"
        ));
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ========== Role Management ==========

    @Transactional
    public void changeUserRole(UUID userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() == UserRole.SUPER_ADMIN) {
            throw new IllegalStateException("Cannot change role of super admin accounts");
        }

        if (newRole == UserRole.SUPER_ADMIN) {
            throw new IllegalStateException("Cannot promote to super admin role via this interface");
        }

        UserRole oldRole = user.getRole();
        user.changeRole(newRole);
        userRepository.save(user);

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.USER_ROLE_CHANGED,
                "USER",
                userId,
                null,
                "Role changed from " + oldRole + " to " + newRole + " for user: " + user.getEmail()
        ));
    }

    // ========== Bulk Operations ==========

    @Transactional
    public BulkOperationResult bulkSuspend(List<UUID> userIds) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (UUID userId : userIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    failed++;
                    errors.add("User not found: " + userId);
                    continue;
                }
                if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
                    failed++;
                    errors.add("Cannot suspend admin: " + user.getEmail());
                    continue;
                }
                user.suspend();
                userRepository.save(user);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Error suspending " + userId + ": " + e.getMessage());
            }
        }

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.USER_BULK_ACTION,
                "USER",
                null,
                null,
                "Bulk suspend: " + success + " succeeded, " + failed + " failed"
        ));

        return new BulkOperationResult(success, failed, errors);
    }

    @Transactional
    public BulkOperationResult bulkReactivate(List<UUID> userIds) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (UUID userId : userIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    failed++;
                    errors.add("User not found: " + userId);
                    continue;
                }
                user.reactivate();
                userRepository.save(user);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Error reactivating " + userId + ": " + e.getMessage());
            }
        }

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.USER_BULK_ACTION,
                "USER",
                null,
                null,
                "Bulk reactivate: " + success + " succeeded, " + failed + " failed"
        ));

        return new BulkOperationResult(success, failed, errors);
    }

    @Transactional
    public BulkOperationResult bulkDelete(List<UUID> userIds) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (UUID userId : userIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    failed++;
                    errors.add("User not found: " + userId);
                    continue;
                }
                if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
                    failed++;
                    errors.add("Cannot delete admin: " + user.getEmail());
                    continue;
                }

                // Delete associated rescue org profile if exists
                if (user.getRole() == UserRole.RESCUE_ORG) {
                    rescueOrgRepository.findByUserId(userId).ifPresent(rescueOrgRepository::delete);
                }

                String email = user.getEmail();
                userRepository.delete(user);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Error deleting " + userId + ": " + e.getMessage());
            }
        }

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.USER_BULK_ACTION,
                "USER",
                null,
                null,
                "Bulk delete: " + success + " succeeded, " + failed + " failed"
        ));

        return new BulkOperationResult(success, failed, errors);
    }

    @Transactional
    public BulkOperationResult bulkChangeRole(List<UUID> userIds, UserRole newRole) {
        if (newRole == UserRole.SUPER_ADMIN) {
            return new BulkOperationResult(0, userIds.size(), List.of("Cannot promote to super admin role"));
        }

        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (UUID userId : userIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    failed++;
                    errors.add("User not found: " + userId);
                    continue;
                }
                if (user.getRole() == UserRole.SUPER_ADMIN) {
                    failed++;
                    errors.add("Cannot change super admin role: " + user.getEmail());
                    continue;
                }
                user.changeRole(newRole);
                userRepository.save(user);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Error changing role for " + userId + ": " + e.getMessage());
            }
        }

        auditLogRepository.save(AuditLog.create(
                AuditLog.AuditAction.USER_BULK_ACTION,
                "USER",
                null,
                null,
                "Bulk role change to " + newRole + ": " + success + " succeeded, " + failed + " failed"
        ));

        return new BulkOperationResult(success, failed, errors);
    }

    public record UserSearchResult(
            List<User> users,
            int page,
            int size,
            long totalCount,
            int totalPages
    ) {}

    public record BulkOperationResult(
            int successCount,
            int failedCount,
            List<String> errors
    ) {}
}
