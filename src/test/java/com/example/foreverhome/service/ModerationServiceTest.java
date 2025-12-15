package com.example.foreverhome.service;

import com.example.foreverhome.domain.moderation.AuditLog;
import com.example.foreverhome.domain.moderation.AuditLog.AuditAction;
import com.example.foreverhome.domain.moderation.ContentFlag;
import com.example.foreverhome.domain.moderation.ContentFlag.ContentType;
import com.example.foreverhome.domain.moderation.ContentFlag.FlagReason;
import com.example.foreverhome.domain.moderation.ContentFlag.FlagStatus;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.logging.UserJourneyLogger;
import com.example.foreverhome.repository.AuditLogRepository;
import com.example.foreverhome.repository.ContentFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModerationService")
class ModerationServiceTest {

    @Mock
    private ContentFlagRepository contentFlagRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserJourneyLogger journeyLogger;

    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        moderationService = new ModerationService(contentFlagRepository, auditLogRepository, journeyLogger);
    }

    @Nested
    @DisplayName("createFlag")
    class CreateFlag {

        @Test
        @DisplayName("given valid flag data, when createFlag, then creates and saves flag")
        void givenValidFlagData_whenCreateFlag_thenCreatesAndSavesFlag() {
            // Given
            ContentType contentType = ContentType.PET;
            UUID contentId = UUID.randomUUID();
            UUID reporterId = UUID.randomUUID();
            FlagReason reason = FlagReason.INAPPROPRIATE_CONTENT;
            String description = "Inappropriate pet description";

            when(contentFlagRepository.save(any(ContentFlag.class))).thenAnswer(inv -> {
                ContentFlag flag = inv.getArgument(0);
                setId(flag, UUID.randomUUID());
                return flag;
            });

            // When
            ContentFlag result = moderationService.createFlag(contentType, contentId, reporterId, reason, description);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContentType()).isEqualTo(contentType);
            assertThat(result.getContentId()).isEqualTo(contentId);
            assertThat(result.getReporterId()).isEqualTo(reporterId);
            assertThat(result.getReason()).isEqualTo(reason);
            assertThat(result.getDescription()).isEqualTo(description);
            assertThat(result.getStatus()).isEqualTo(FlagStatus.PENDING);

            verify(contentFlagRepository).save(any(ContentFlag.class));
            verify(journeyLogger).logModeration(eq(UserJourneyLogger.ACTION_FLAG_CREATE), any(), anyString(), eq(contentId), anyString());
        }

        @Test
        @DisplayName("given different content types, when createFlag, then creates flag for each type")
        void givenDifferentContentTypes_whenCreateFlag_thenCreatesFlagForEachType() {
            // Given
            UUID contentId = UUID.randomUUID();
            UUID reporterId = UUID.randomUUID();
            FlagReason reason = FlagReason.FRAUD;

            when(contentFlagRepository.save(any(ContentFlag.class))).thenAnswer(inv -> {
                ContentFlag flag = inv.getArgument(0);
                setId(flag, UUID.randomUUID());
                return flag;
            });

            // When - Create flags for different content types
            ContentFlag petFlag = moderationService.createFlag(ContentType.PET, contentId, reporterId, reason, "Test");
            ContentFlag userFlag = moderationService.createFlag(ContentType.USER, contentId, reporterId, reason, "Test");

            // Then
            assertThat(petFlag.getContentType()).isEqualTo(ContentType.PET);
            assertThat(userFlag.getContentType()).isEqualTo(ContentType.USER);
        }
    }

    @Nested
    @DisplayName("getPendingFlags")
    class GetPendingFlags {

        @Test
        @DisplayName("given pending flags exist, when getPendingFlags, then returns list of pending flags")
        void givenPendingFlagsExist_whenGetPendingFlags_thenReturnsListOfPendingFlags() {
            // Given
            ContentFlag flag1 = createFlag(ContentType.PET, FlagStatus.PENDING);
            ContentFlag flag2 = createFlag(ContentType.USER, FlagStatus.PENDING);
            when(contentFlagRepository.findPendingFlags()).thenReturn(List.of(flag1, flag2));

            // When
            List<ContentFlag> result = moderationService.getPendingFlags();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(flag -> flag.getStatus() == FlagStatus.PENDING);
        }

        @Test
        @DisplayName("given no pending flags, when getPendingFlags, then returns empty list")
        void givenNoPendingFlags_whenGetPendingFlags_thenReturnsEmptyList() {
            // Given
            when(contentFlagRepository.findPendingFlags()).thenReturn(List.of());

            // When
            List<ContentFlag> result = moderationService.getPendingFlags();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFlags")
    class GetFlags {

        @Test
        @DisplayName("given status filter, when getFlags, then returns flags with that status")
        void givenStatusFilter_whenGetFlags_thenReturnsFlagsWithThatStatus() {
            // Given
            ContentFlag flag = createFlag(ContentType.PET, FlagStatus.APPROVED);
            when(contentFlagRepository.findByStatusPaged(FlagStatus.APPROVED, 10, 0)).thenReturn(List.of(flag));

            // When
            List<ContentFlag> result = moderationService.getFlags(FlagStatus.APPROVED, 1, 10);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(FlagStatus.APPROVED);
        }

        @Test
        @DisplayName("given no status filter, when getFlags, then returns all flags paged")
        void givenNoStatusFilter_whenGetFlags_thenReturnsAllFlagsPaged() {
            // Given
            ContentFlag flag1 = createFlag(ContentType.PET, FlagStatus.PENDING);
            ContentFlag flag2 = createFlag(ContentType.USER, FlagStatus.APPROVED);
            when(contentFlagRepository.findAllPaged(10, 0)).thenReturn(List.of(flag1, flag2));

            // When
            List<ContentFlag> result = moderationService.getFlags(null, 1, 10);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("given page 2, when getFlags, then calculates correct offset")
        void givenPage2_whenGetFlags_thenCalculatesCorrectOffset() {
            // Given
            when(contentFlagRepository.findAllPaged(10, 10)).thenReturn(List.of());

            // When
            moderationService.getFlags(null, 2, 10);

            // Then
            verify(contentFlagRepository).findAllPaged(10, 10); // offset = (2-1) * 10 = 10
        }
    }

    @Nested
    @DisplayName("countFlags")
    class CountFlags {

        @Test
        @DisplayName("given status filter, when countFlags, then returns count for that status")
        void givenStatusFilter_whenCountFlags_thenReturnsCountForThatStatus() {
            // Given
            when(contentFlagRepository.countByStatus(FlagStatus.PENDING)).thenReturn(5L);

            // When
            long result = moderationService.countFlags(FlagStatus.PENDING);

            // Then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("given no status filter, when countFlags, then returns total count")
        void givenNoStatusFilter_whenCountFlags_thenReturnsTotalCount() {
            // Given
            when(contentFlagRepository.count()).thenReturn(15L);

            // When
            long result = moderationService.countFlags(null);

            // Then
            assertThat(result).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("approveFlag")
    class ApproveFlag {

        @Test
        @DisplayName("given pending flag, when approveFlag, then approves and logs action")
        void givenPendingFlag_whenApproveFlag_thenApprovesAndLogsAction() {
            // Given
            UUID flagId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            String notes = "Content removed";
            ContentFlag flag = createFlag(ContentType.PET, FlagStatus.PENDING);
            setId(flag, flagId);

            when(contentFlagRepository.findById(flagId)).thenReturn(Optional.of(flag));
            when(contentFlagRepository.save(any(ContentFlag.class))).thenAnswer(inv -> inv.getArgument(0));
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ContentFlag result = moderationService.approveFlag(flagId, reviewerId, notes);

            // Then
            assertThat(result.getStatus()).isEqualTo(FlagStatus.APPROVED);
            assertThat(result.getReviewedBy()).isEqualTo(reviewerId);
            assertThat(result.getResolutionNotes()).isEqualTo(notes);

            verify(auditLogRepository).save(argThat(log -> log.getAction() == AuditAction.CONTENT_FLAG_APPROVED));
            verify(journeyLogger).logModeration(eq(UserJourneyLogger.ACTION_FLAG_APPROVE), eq(flagId), anyString(), any(), eq(notes));
        }

        @Test
        @DisplayName("given non-existent flag, when approveFlag, then throws ResourceNotFoundException")
        void givenNonExistentFlag_whenApproveFlag_thenThrowsResourceNotFoundException() {
            // Given
            UUID flagId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();

            when(contentFlagRepository.findById(flagId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> moderationService.approveFlag(flagId, reviewerId, "notes"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ContentFlag");
        }
    }

    @Nested
    @DisplayName("dismissFlag")
    class DismissFlag {

        @Test
        @DisplayName("given pending flag, when dismissFlag, then dismisses and logs action")
        void givenPendingFlag_whenDismissFlag_thenDismissesAndLogsAction() {
            // Given
            UUID flagId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            String notes = "False positive";
            ContentFlag flag = createFlag(ContentType.PET, FlagStatus.PENDING);
            setId(flag, flagId);

            when(contentFlagRepository.findById(flagId)).thenReturn(Optional.of(flag));
            when(contentFlagRepository.save(any(ContentFlag.class))).thenAnswer(inv -> inv.getArgument(0));
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ContentFlag result = moderationService.dismissFlag(flagId, reviewerId, notes);

            // Then
            assertThat(result.getStatus()).isEqualTo(FlagStatus.DISMISSED);
            assertThat(result.getReviewedBy()).isEqualTo(reviewerId);
            assertThat(result.getResolutionNotes()).isEqualTo(notes);

            verify(auditLogRepository).save(argThat(log -> log.getAction() == AuditAction.CONTENT_FLAG_DISMISSED));
            verify(journeyLogger).logModeration(eq(UserJourneyLogger.ACTION_FLAG_DISMISS), eq(flagId), anyString(), any(), eq(notes));
        }

        @Test
        @DisplayName("given non-existent flag, when dismissFlag, then throws ResourceNotFoundException")
        void givenNonExistentFlag_whenDismissFlag_thenThrowsResourceNotFoundException() {
            // Given
            UUID flagId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();

            when(contentFlagRepository.findById(flagId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> moderationService.dismissFlag(flagId, reviewerId, "notes"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ContentFlag");
        }
    }

    @Nested
    @DisplayName("logAuditAction")
    class LogAuditAction {

        @Test
        @DisplayName("given audit action details, when logAuditAction, then creates and saves audit log")
        void givenAuditActionDetails_whenLogAuditAction_thenCreatesAndSavesAuditLog() {
            // Given
            AuditAction action = AuditAction.USER_SUSPENDED;
            String entityType = "User";
            UUID entityId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();
            String details = "User suspended for policy violation";
            String ipAddress = "192.168.1.1";

            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            moderationService.logAuditAction(action, entityType, entityId, actorId, details, ipAddress);

            // Then
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog savedLog = captor.getValue();
            assertThat(savedLog.getAction()).isEqualTo(action);
            assertThat(savedLog.getEntityType()).isEqualTo(entityType);
            assertThat(savedLog.getEntityId()).isEqualTo(entityId);
            assertThat(savedLog.getActorId()).isEqualTo(actorId);
            assertThat(savedLog.getDetails()).isEqualTo(details);
        }
    }

    @Nested
    @DisplayName("getAuditLogs")
    class GetAuditLogs {

        @Test
        @DisplayName("given audit logs exist, when getAuditLogs, then returns paged list")
        void givenAuditLogsExist_whenGetAuditLogs_thenReturnsPagedList() {
            // Given
            AuditLog log1 = AuditLog.create(AuditAction.USER_SUSPENDED, "User", UUID.randomUUID(), UUID.randomUUID(), "Details", null);
            AuditLog log2 = AuditLog.create(AuditAction.USER_REACTIVATED, "User", UUID.randomUUID(), UUID.randomUUID(), "Details", null);
            when(auditLogRepository.findAllPaged(10, 0)).thenReturn(List.of(log1, log2));

            // When
            List<AuditLog> result = moderationService.getAuditLogs(1, 10);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getAuditLogsForEntity")
    class GetAuditLogsForEntity {

        @Test
        @DisplayName("given entity with audit logs, when getAuditLogsForEntity, then returns logs for that entity")
        void givenEntityWithAuditLogs_whenGetAuditLogsForEntity_thenReturnsLogsForThatEntity() {
            // Given
            String entityType = "User";
            UUID entityId = UUID.randomUUID();
            AuditLog log = AuditLog.create(AuditAction.USER_SUSPENDED, entityType, entityId, UUID.randomUUID(), "Details", null);
            when(auditLogRepository.findByEntity(entityType, entityId)).thenReturn(List.of(log));

            // When
            List<AuditLog> result = moderationService.getAuditLogsForEntity(entityType, entityId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntityId()).isEqualTo(entityId);
        }
    }

    // Helper methods
    private ContentFlag createFlag(ContentType contentType, FlagStatus status) {
        ContentFlag flag = ContentFlag.create(contentType, UUID.randomUUID(), UUID.randomUUID(),
                FlagReason.INAPPROPRIATE_CONTENT, "Test description");
        if (status == FlagStatus.APPROVED) {
            flag.approve(UUID.randomUUID(), "Approved");
        } else if (status == FlagStatus.DISMISSED) {
            flag.dismiss(UUID.randomUUID(), "Dismissed");
        }
        return flag;
    }

    private void setId(Object entity, UUID id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }
}
