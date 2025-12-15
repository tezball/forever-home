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
import com.example.foreverhome.repository.AdoptionRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.UserRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.EmailService;
import com.example.foreverhome.service.ModerationService;
import com.example.foreverhome.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private AdoptionRepository adoptionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private ModerationService moderationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserJourneyLogger journeyLogger;

    @InjectMocks
    private AdminController adminController;

    private UserPrincipal adminPrincipal;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        adminUserId = UUID.randomUUID();
        adminPrincipal = new UserPrincipal(adminUserId, UserRole.ADMIN, true);
    }

    @Nested
    @DisplayName("GET /api/admin/analytics")
    class GetAnalytics {

        @Test
        @DisplayName("should return analytics with all counts")
        void shouldReturnAnalyticsWithAllCounts() {
            // Given
            when(userRepository.count()).thenReturn(100L);
            when(petRepository.count()).thenReturn(50L);
            when(adoptionRepository.countAll()).thenReturn(25L);
            when(userRepository.findByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING))
                    .thenReturn(List.of(createTestUser(UserRole.RESCUE_ORG, AccountStatus.PENDING)));

            when(userRepository.countByRole(UserRole.ADOPTER)).thenReturn(60L);
            when(userRepository.countByRole(UserRole.FOSTER)).thenReturn(20L);
            when(userRepository.countByRole(UserRole.RESCUE_ORG)).thenReturn(10L);
            when(userRepository.countByRole(UserRole.VET)).thenReturn(8L);
            when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);

            when(petRepository.countByStatus(PetStatus.AVAILABLE)).thenReturn(15L);
            when(petRepository.countByStatus(PetStatus.PENDING_RESCUE)).thenReturn(5L);
            when(petRepository.countByStatus(PetStatus.PENDING_VET)).thenReturn(3L);
            when(petRepository.countByStatus(PetStatus.IN_PROGRESS)).thenReturn(2L);
            when(petRepository.countByStatus(PetStatus.ADOPTED)).thenReturn(20L);
            when(petRepository.countByStatus(PetStatus.DRAFT)).thenReturn(4L);
            when(petRepository.countByStatus(PetStatus.ON_HOLD)).thenReturn(0L);
            when(petRepository.countByStatus(PetStatus.WITHDRAWN)).thenReturn(1L);

            // When
            ResponseEntity<AdminController.AnalyticsResponse> response = adminController.getAnalytics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().totalUsers()).isEqualTo(100L);
            assertThat(response.getBody().totalPets()).isEqualTo(50L);
            assertThat(response.getBody().totalAdoptions()).isEqualTo(25L);
            assertThat(response.getBody().pendingApprovals()).isEqualTo(1L);
            assertThat(response.getBody().userDistribution().adopters()).isEqualTo(60L);
            assertThat(response.getBody().petStatistics().available()).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/approvals")
    class GetPendingApprovals {

        @Test
        @DisplayName("should return pending rescue org approvals")
        void shouldReturnPendingRescueOrgApprovals() {
            // Given
            User pendingRescue = createTestUser(UserRole.RESCUE_ORG, AccountStatus.PENDING);
            when(userRepository.findByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING))
                    .thenReturn(List.of(pendingRescue));

            // When
            ResponseEntity<List<AdminController.ApprovalResponse>> response = adminController.getPendingApprovals();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).type()).isEqualTo("RESCUE_ORG");
        }

        @Test
        @DisplayName("should return empty list when no pending approvals")
        void shouldReturnEmptyListWhenNoPendingApprovals() {
            // Given
            when(userRepository.findByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING))
                    .thenReturn(List.of());

            // When
            ResponseEntity<List<AdminController.ApprovalResponse>> response = adminController.getPendingApprovals();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users")
    class GetUsers {

        @Test
        @DisplayName("should return all users")
        void shouldReturnAllUsers() {
            // Given
            User user1 = createTestUser(UserRole.ADOPTER, AccountStatus.ACTIVE);
            User user2 = createTestUser(UserRole.FOSTER, AccountStatus.ACTIVE);
            when(userRepository.findAll()).thenReturn(List.of(user1, user2));

            // When
            ResponseEntity<List<AdminController.UserResponse>> response = adminController.getUsers();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/approvals/{type}/{id}/approve")
    class ApproveUser {

        @Test
        @DisplayName("should approve rescue org and activate account")
        void shouldApproveRescueOrgAndActivateAccount() {
            // Given
            UUID userId = UUID.randomUUID();
            User pendingRescue = createTestUser(UserRole.RESCUE_ORG, AccountStatus.PENDING);
            when(userRepository.findById(userId)).thenReturn(Optional.of(pendingRescue));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<Void> response = adminController.approveUser("RESCUE_ORG", userId.toString());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);

            verify(journeyLogger).logAdminAction(eq(UserJourneyLogger.ACTION_ADMIN_APPROVE_RESCUE),
                    eq("USER"), eq(userId), contains("Approved"));
            verify(notificationService).notifyRescueOrgVerified(userId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent user")
        void shouldThrowResourceNotFoundExceptionForNonExistentUser() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminController.approveUser("RESCUE_ORG", userId.toString()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/approvals/{type}/{id}/reject")
    class RejectUser {

        @Test
        @DisplayName("should reject rescue org and suspend account")
        void shouldRejectRescueOrgAndSuspendAccount() {
            // Given - must use ACTIVE status since PENDING->SUSPENDED is not a valid transition
            UUID userId = UUID.randomUUID();
            User activeRescue = createTestUser(UserRole.RESCUE_ORG, AccountStatus.ACTIVE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(activeRescue));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AdminController.RejectRequest request = new AdminController.RejectRequest("Not enough documentation");

            // When
            ResponseEntity<Void> response = adminController.rejectUser("RESCUE_ORG", userId.toString(), request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo(AccountStatus.SUSPENDED);

            verify(notificationService).notifyRescueOrgRejected(userId, "Not enough documentation");
        }

        @Test
        @DisplayName("should reject without reason when request is null")
        void shouldRejectWithoutReasonWhenRequestIsNull() {
            // Given - must use ACTIVE status since PENDING->SUSPENDED is not a valid transition
            UUID userId = UUID.randomUUID();
            User activeRescue = createTestUser(UserRole.RESCUE_ORG, AccountStatus.ACTIVE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(activeRescue));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<Void> response = adminController.rejectUser("RESCUE_ORG", userId.toString(), null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(notificationService).notifyRescueOrgRejected(eq(userId), isNull());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/users/{id}/suspend")
    class SuspendUser {

        @Test
        @DisplayName("should suspend active user")
        void shouldSuspendActiveUser() {
            // Given
            UUID userId = UUID.randomUUID();
            User activeUser = createTestUser(UserRole.ADOPTER, AccountStatus.ACTIVE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<Void> response = adminController.suspendUser(userId.toString());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo(AccountStatus.SUSPENDED);

            verify(journeyLogger).logAdminAction(eq(UserJourneyLogger.ACTION_ADMIN_SUSPEND_USER),
                    eq("USER"), eq(userId), contains("Suspended"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent user")
        void shouldThrowResourceNotFoundExceptionForNonExistentUser() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminController.suspendUser(userId.toString()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/users/{id}/reactivate")
    class ReactivateUser {

        @Test
        @DisplayName("should reactivate suspended user")
        void shouldReactivateSuspendedUser() {
            // Given
            UUID userId = UUID.randomUUID();
            User suspendedUser = createTestUser(UserRole.ADOPTER, AccountStatus.SUSPENDED);
            when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<Void> response = adminController.reactivateUser(userId.toString());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);

            verify(journeyLogger).logAdminAction(eq(UserJourneyLogger.ACTION_ADMIN_REACTIVATE_USER),
                    eq("USER"), eq(userId), contains("Reactivated"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users/search")
    class SearchUsers {

        @Test
        @DisplayName("should search users by query")
        void shouldSearchUsersByQuery() {
            // Given
            User user = createTestUser(UserRole.ADOPTER, AccountStatus.ACTIVE);
            when(userRepository.searchByNameOrEmail(eq("%john%"), eq(20), eq(0))).thenReturn(List.of(user));
            when(userRepository.countSearchByNameOrEmail(eq("%john%"))).thenReturn(1L);

            // When
            ResponseEntity<AdminController.UserSearchResponse> response =
                    adminController.searchUsers("john", null, null, 1, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().users()).hasSize(1);
            assertThat(response.getBody().totalCount()).isEqualTo(1L);
            assertThat(response.getBody().page()).isEqualTo(1);
        }

        @Test
        @DisplayName("should filter by role and status")
        void shouldFilterByRoleAndStatus() {
            // Given
            User user = createTestUser(UserRole.RESCUE_ORG, AccountStatus.PENDING);
            when(userRepository.findByRoleAndStatusPaged(UserRole.RESCUE_ORG, AccountStatus.PENDING, 20, 0))
                    .thenReturn(List.of(user));
            when(userRepository.countByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING)).thenReturn(1L);

            // When
            ResponseEntity<AdminController.UserSearchResponse> response =
                    adminController.searchUsers(null, "RESCUE_ORG", "PENDING", 1, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().users()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by role only")
        void shouldFilterByRoleOnly() {
            // Given
            User user = createTestUser(UserRole.VET, AccountStatus.ACTIVE);
            when(userRepository.findByRolePaged(UserRole.VET, 20, 0)).thenReturn(List.of(user));
            when(userRepository.countByRole(UserRole.VET)).thenReturn(1L);

            // When
            ResponseEntity<AdminController.UserSearchResponse> response =
                    adminController.searchUsers(null, "VET", null, 1, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().users()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by status only")
        void shouldFilterByStatusOnly() {
            // Given
            User user = createTestUser(UserRole.ADOPTER, AccountStatus.PENDING);
            when(userRepository.findByStatusPaged(AccountStatus.PENDING, 20, 0)).thenReturn(List.of(user));
            when(userRepository.countByStatus(AccountStatus.PENDING)).thenReturn(1L);

            // When
            ResponseEntity<AdminController.UserSearchResponse> response =
                    adminController.searchUsers(null, null, "PENDING", 1, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().users()).hasSize(1);
        }

        @Test
        @DisplayName("should return all users when no filters")
        void shouldReturnAllUsersWhenNoFilters() {
            // Given
            User user = createTestUser(UserRole.ADOPTER, AccountStatus.ACTIVE);
            when(userRepository.findAllPaged(20, 0)).thenReturn(List.of(user));
            when(userRepository.count()).thenReturn(1L);

            // When
            ResponseEntity<AdminController.UserSearchResponse> response =
                    adminController.searchUsers(null, null, null, 1, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().users()).hasSize(1);
        }

        @Test
        @DisplayName("should calculate total pages correctly")
        void shouldCalculateTotalPagesCorrectly() {
            // Given
            when(userRepository.findAllPaged(20, 0)).thenReturn(List.of());
            when(userRepository.count()).thenReturn(45L);

            // When
            ResponseEntity<AdminController.UserSearchResponse> response =
                    adminController.searchUsers(null, null, null, 1, 20);

            // Then
            assertThat(response.getBody().totalPages()).isEqualTo(3); // ceil(45/20) = 3
        }

        @Test
        @DisplayName("should limit page size to 100")
        void shouldLimitPageSizeTo100() {
            // Given
            when(userRepository.findAllPaged(100, 0)).thenReturn(List.of());
            when(userRepository.count()).thenReturn(0L);

            // When
            adminController.searchUsers(null, null, null, 1, 200);

            // Then
            verify(userRepository).findAllPaged(100, 0); // Should be limited to 100
        }
    }

    @Nested
    @DisplayName("POST /api/admin/users/{id}/reset-password")
    class ResetUserPassword {

        @Test
        @DisplayName("should generate temp password and send email")
        void shouldGenerateTempPasswordAndSendEmail() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = createTestUser(UserRole.ADOPTER, AccountStatus.ACTIVE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

            // When
            ResponseEntity<AdminController.ResetPasswordResponse> response =
                    adminController.resetUserPassword(userId.toString());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().message()).contains("Password reset email sent");

            verify(passwordEncoder).encode(argThat(password ->
                    password != null && password.length() == 12
            ));
            verify(emailService).sendPasswordResetByAdmin(eq(user.getEmail()), anyString());
            verify(journeyLogger).logAdminAction(eq(UserJourneyLogger.ACTION_ADMIN_RESET_PASSWORD),
                    eq("USER"), eq(userId), contains("Password reset"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent user")
        void shouldThrowResourceNotFoundExceptionForNonExistentUser() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminController.resetUserPassword(userId.toString()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/flags")
    class GetFlags {

        @Test
        @DisplayName("should return flags with pagination")
        void shouldReturnFlagsWithPagination() {
            // Given
            ContentFlag flag = mock(ContentFlag.class);
            when(flag.getId()).thenReturn(UUID.randomUUID());
            when(flag.getContentType()).thenReturn(ContentType.PET);
            when(flag.getContentId()).thenReturn(UUID.randomUUID());
            when(flag.getReason()).thenReturn(FlagReason.INAPPROPRIATE_CONTENT);
            when(flag.getStatus()).thenReturn(FlagStatus.PENDING);
            when(flag.getCreatedAt()).thenReturn(java.time.Instant.now());

            when(moderationService.getFlags(null, 1, 20)).thenReturn(List.of(flag));
            when(moderationService.countFlags(null)).thenReturn(1L);

            // When
            ResponseEntity<AdminController.FlagSearchResponse> response =
                    adminController.getFlags(null, 1, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().flags()).hasSize(1);
            assertThat(response.getBody().totalCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should filter flags by status")
        void shouldFilterFlagsByStatus() {
            // Given
            when(moderationService.getFlags(FlagStatus.PENDING, 1, 20)).thenReturn(List.of());
            when(moderationService.countFlags(FlagStatus.PENDING)).thenReturn(0L);

            // When
            ResponseEntity<AdminController.FlagSearchResponse> response =
                    adminController.getFlags("PENDING", 1, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(moderationService).getFlags(FlagStatus.PENDING, 1, 20);
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/flags/{id}/approve")
    class ApproveFlag {

        @Test
        @DisplayName("should approve flag and return updated flag")
        void shouldApproveFlagAndReturnUpdatedFlag() {
            // Given
            UUID flagId = UUID.randomUUID();
            ContentFlag flag = mock(ContentFlag.class);
            when(flag.getId()).thenReturn(flagId);
            when(flag.getContentType()).thenReturn(ContentType.PET);
            when(flag.getContentId()).thenReturn(UUID.randomUUID());
            when(flag.getReason()).thenReturn(FlagReason.INAPPROPRIATE_CONTENT);
            when(flag.getStatus()).thenReturn(FlagStatus.APPROVED);
            when(flag.getCreatedAt()).thenReturn(java.time.Instant.now());

            when(moderationService.approveFlag(flagId, adminUserId, "Confirmed inappropriate"))
                    .thenReturn(flag);

            AdminController.FlagResolveRequest request = new AdminController.FlagResolveRequest("Confirmed inappropriate");

            // When
            ResponseEntity<AdminController.FlagResponse> response =
                    adminController.approveFlag(flagId.toString(), request, adminPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("APPROVED");
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/flags/{id}/dismiss")
    class DismissFlag {

        @Test
        @DisplayName("should dismiss flag and return updated flag")
        void shouldDismissFlagAndReturnUpdatedFlag() {
            // Given
            UUID flagId = UUID.randomUUID();
            ContentFlag flag = mock(ContentFlag.class);
            when(flag.getId()).thenReturn(flagId);
            when(flag.getContentType()).thenReturn(ContentType.PET);
            when(flag.getContentId()).thenReturn(UUID.randomUUID());
            when(flag.getReason()).thenReturn(FlagReason.SPAM);
            when(flag.getStatus()).thenReturn(FlagStatus.DISMISSED);
            when(flag.getCreatedAt()).thenReturn(java.time.Instant.now());

            when(moderationService.dismissFlag(flagId, adminUserId, "False positive"))
                    .thenReturn(flag);

            AdminController.FlagResolveRequest request = new AdminController.FlagResolveRequest("False positive");

            // When
            ResponseEntity<AdminController.FlagResponse> response =
                    adminController.dismissFlag(flagId.toString(), request, adminPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo("DISMISSED");
        }
    }

    @Nested
    @DisplayName("GET /api/admin/audit-logs")
    class GetAuditLogs {

        @Test
        @DisplayName("should return audit logs with pagination")
        void shouldReturnAuditLogsWithPagination() {
            // Given
            AuditLog log = mock(AuditLog.class);
            when(log.getId()).thenReturn(UUID.randomUUID());
            when(log.getAction()).thenReturn(AuditLog.AuditAction.RESCUE_ORG_APPROVED);
            when(log.getEntityType()).thenReturn("USER");
            when(log.getCreatedAt()).thenReturn(java.time.Instant.now());

            when(moderationService.getAuditLogs(1, 50)).thenReturn(List.of(log));

            // When
            ResponseEntity<List<AdminController.AuditLogResponse>> response =
                    adminController.getAuditLogs(1, 50);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    // Helper methods

    private User createTestUser(UserRole role, AccountStatus status) {
        User user = User.create("test@example.com", "hashedPassword", role);

        // Set status using reflection
        try {
            java.lang.reflect.Field statusField = User.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(user, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return user;
    }
}
