package com.example.foreverhome.service;

import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.verification.VetApproval;
import com.example.foreverhome.domain.verification.VetApprovalRequest;
import com.example.foreverhome.domain.verification.VetApprovalRequestStatus;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.VetApprovalRepository;
import com.example.foreverhome.repository.VetApprovalRequestRepository;
import com.example.foreverhome.repository.VetRepository;
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
@DisplayName("VetApprovalRequestService")
class VetApprovalRequestServiceTest {

    @Mock
    private VetApprovalRequestRepository requestRepository;

    @Mock
    private VetApprovalRepository approvalRepository;

    @Mock
    private VetRepository vetRepository;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @Mock
    private NotificationService notificationService;

    private VetApprovalRequestService vetApprovalRequestService;

    @BeforeEach
    void setUp() {
        vetApprovalRequestService = new VetApprovalRequestService(
                requestRepository,
                approvalRepository,
                vetRepository,
                rescueOrganizationRepository,
                notificationService
        );
    }

    @Nested
    @DisplayName("requestApproval")
    class RequestApproval {

        @Test
        @DisplayName("given valid vet and rescue org, when requestApproval, then creates request and notifies")
        void givenValidVetAndRescueOrg_whenRequestApproval_thenCreatesRequestAndNotifies() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            String message = "I'd like to work with your organization";
            Vet vet = createVet(vetUserId);
            RescueOrganization rescueOrg = createRescueOrg(rescueOrgId, true);

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
            when(rescueOrganizationRepository.findById(rescueOrgId)).thenReturn(Optional.of(rescueOrg));
            when(approvalRepository.existsByVetIdAndRescueOrgId(vet.getId(), rescueOrgId)).thenReturn(false);
            when(requestRepository.existsByVetIdAndRescueOrgIdAndStatus(vet.getId(), rescueOrgId, VetApprovalRequestStatus.PENDING)).thenReturn(false);
            when(requestRepository.save(any(VetApprovalRequest.class))).thenAnswer(inv -> {
                VetApprovalRequest req = inv.getArgument(0);
                setId(req, UUID.randomUUID());
                return req;
            });

            // When
            VetApprovalRequest result = vetApprovalRequestService.requestApproval(vetUserId, rescueOrgId, message);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getVetId()).isEqualTo(vet.getId());
            assertThat(result.getRescueOrgId()).isEqualTo(rescueOrgId);
            assertThat(result.getMessage()).isEqualTo(message);
            assertThat(result.getStatus()).isEqualTo(VetApprovalRequestStatus.PENDING);

            verify(notificationService).notifyRescueOrgVetRequest(rescueOrg.getUserId(), vet.getClinicName());
        }

        @Test
        @DisplayName("given unverified rescue org, when requestApproval, then throws IllegalArgumentException")
        void givenUnverifiedRescueOrg_whenRequestApproval_thenThrowsIllegalArgumentException() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            Vet vet = createVet(vetUserId);
            RescueOrganization rescueOrg = createRescueOrg(rescueOrgId, false);

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
            when(rescueOrganizationRepository.findById(rescueOrgId)).thenReturn(Optional.of(rescueOrg));

            // When/Then
            assertThatThrownBy(() -> vetApprovalRequestService.requestApproval(vetUserId, rescueOrgId, "message"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unverified");
        }

        @Test
        @DisplayName("given already approved vet, when requestApproval, then throws IllegalArgumentException")
        void givenAlreadyApprovedVet_whenRequestApproval_thenThrowsIllegalArgumentException() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            Vet vet = createVet(vetUserId);
            RescueOrganization rescueOrg = createRescueOrg(rescueOrgId, true);

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
            when(rescueOrganizationRepository.findById(rescueOrgId)).thenReturn(Optional.of(rescueOrg));
            when(approvalRepository.existsByVetIdAndRescueOrgId(vet.getId(), rescueOrgId)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> vetApprovalRequestService.requestApproval(vetUserId, rescueOrgId, "message"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already approved");
        }

        @Test
        @DisplayName("given pending request exists, when requestApproval, then throws IllegalArgumentException")
        void givenPendingRequestExists_whenRequestApproval_thenThrowsIllegalArgumentException() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            Vet vet = createVet(vetUserId);
            RescueOrganization rescueOrg = createRescueOrg(rescueOrgId, true);

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
            when(rescueOrganizationRepository.findById(rescueOrgId)).thenReturn(Optional.of(rescueOrg));
            when(approvalRepository.existsByVetIdAndRescueOrgId(vet.getId(), rescueOrgId)).thenReturn(false);
            when(requestRepository.existsByVetIdAndRescueOrgIdAndStatus(vet.getId(), rescueOrgId, VetApprovalRequestStatus.PENDING)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> vetApprovalRequestService.requestApproval(vetUserId, rescueOrgId, "message"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pending request");
        }

        @Test
        @DisplayName("given vet profile not found, when requestApproval, then throws ResourceNotFoundException")
        void givenVetProfileNotFound_whenRequestApproval_thenThrowsResourceNotFoundException() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> vetApprovalRequestService.requestApproval(vetUserId, rescueOrgId, "message"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Vet profile");
        }
    }

    @Nested
    @DisplayName("approveRequest")
    class ApproveRequest {

        @Test
        @DisplayName("given valid request, when approveRequest, then approves and creates VetApproval")
        void givenValidRequest_whenApproveRequest_thenApprovesAndCreatesVetApproval() {
            // Given
            UUID rescueOrgUserId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID vetId = UUID.randomUUID();
            RescueOrganization rescueOrg = createRescueOrg(UUID.randomUUID(), true);
            setUserId(rescueOrg, rescueOrgUserId);
            VetApprovalRequest request = createRequest(vetId, rescueOrg.getId());
            setId(request, requestId);
            Vet vet = createVet(UUID.randomUUID());
            setId(vet, vetId);

            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(rescueOrg));
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(approvalRepository.save(any(VetApproval.class))).thenAnswer(inv -> {
                VetApproval approval = inv.getArgument(0);
                setId(approval, UUID.randomUUID());
                return approval;
            });
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(vet));
            when(vetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            VetApproval result = vetApprovalRequestService.approveRequest(rescueOrgUserId, requestId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getVetId()).isEqualTo(vetId);
            assertThat(result.getRescueOrgId()).isEqualTo(rescueOrg.getId());

            verify(notificationService).notifyVetRequestApproved(vet.getUserId(), rescueOrg.getName());
        }

        @Test
        @DisplayName("given request not belonging to rescue org, when approveRequest, then throws AccessDeniedException")
        void givenRequestNotBelongingToRescueOrg_whenApproveRequest_thenThrowsAccessDeniedException() {
            // Given
            UUID rescueOrgUserId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            RescueOrganization rescueOrg = createRescueOrg(UUID.randomUUID(), true);
            setUserId(rescueOrg, rescueOrgUserId);
            VetApprovalRequest request = createRequest(UUID.randomUUID(), UUID.randomUUID()); // Different rescue org ID
            setId(request, requestId);

            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(rescueOrg));
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() -> vetApprovalRequestService.approveRequest(rescueOrgUserId, requestId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("given first approval for vet, when approveRequest, then marks vet as verified")
        void givenFirstApprovalForVet_whenApproveRequest_thenMarksVetAsVerified() {
            // Given
            UUID rescueOrgUserId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID vetId = UUID.randomUUID();
            RescueOrganization rescueOrg = createRescueOrg(UUID.randomUUID(), true);
            setUserId(rescueOrg, rescueOrgUserId);
            VetApprovalRequest request = createRequest(vetId, rescueOrg.getId());
            setId(request, requestId);
            Vet vet = createVet(UUID.randomUUID());
            setId(vet, vetId);
            // Vet is not verified

            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(rescueOrg));
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(approvalRepository.save(any(VetApproval.class))).thenAnswer(inv -> {
                VetApproval approval = inv.getArgument(0);
                setId(approval, UUID.randomUUID());
                return approval;
            });
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(vet));
            when(vetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            vetApprovalRequestService.approveRequest(rescueOrgUserId, requestId);

            // Then
            verify(vetRepository).save(argThat(v -> v.isVerified()));
        }
    }

    @Nested
    @DisplayName("rejectRequest")
    class RejectRequest {

        @Test
        @DisplayName("given valid request, when rejectRequest, then rejects and notifies vet")
        void givenValidRequest_whenRejectRequest_thenRejectsAndNotifiesVet() {
            // Given
            UUID rescueOrgUserId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID vetId = UUID.randomUUID();
            String reason = "Not accepting new vets at this time";
            RescueOrganization rescueOrg = createRescueOrg(UUID.randomUUID(), true);
            setUserId(rescueOrg, rescueOrgUserId);
            VetApprovalRequest request = createRequest(vetId, rescueOrg.getId());
            setId(request, requestId);
            Vet vet = createVet(UUID.randomUUID());
            setId(vet, vetId);

            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(rescueOrg));
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(vet));

            // When
            vetApprovalRequestService.rejectRequest(rescueOrgUserId, requestId, reason);

            // Then
            verify(requestRepository).save(argThat(req -> req.getStatus() == VetApprovalRequestStatus.REJECTED));
            verify(notificationService).notifyVetRequestRejected(vet.getUserId(), rescueOrg.getName(), reason);
        }

        @Test
        @DisplayName("given request not belonging to rescue org, when rejectRequest, then throws AccessDeniedException")
        void givenRequestNotBelongingToRescueOrg_whenRejectRequest_thenThrowsAccessDeniedException() {
            // Given
            UUID rescueOrgUserId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            RescueOrganization rescueOrg = createRescueOrg(UUID.randomUUID(), true);
            setUserId(rescueOrg, rescueOrgUserId);
            VetApprovalRequest request = createRequest(UUID.randomUUID(), UUID.randomUUID());
            setId(request, requestId);

            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(rescueOrg));
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() -> vetApprovalRequestService.rejectRequest(rescueOrgUserId, requestId, "reason"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("withdrawRequest")
    class WithdrawRequest {

        @Test
        @DisplayName("given vet's pending request, when withdrawRequest, then withdraws request")
        void givenVetsPendingRequest_whenWithdrawRequest_thenWithdrawsRequest() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            Vet vet = createVet(vetUserId);
            VetApprovalRequest request = createRequest(vet.getId(), UUID.randomUUID());
            setId(request, requestId);

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            vetApprovalRequestService.withdrawRequest(vetUserId, requestId);

            // Then
            verify(requestRepository).save(argThat(req -> req.getStatus() == VetApprovalRequestStatus.WITHDRAWN));
        }

        @Test
        @DisplayName("given request not belonging to vet, when withdrawRequest, then throws AccessDeniedException")
        void givenRequestNotBelongingToVet_whenWithdrawRequest_thenThrowsAccessDeniedException() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            Vet vet = createVet(vetUserId);
            VetApprovalRequest request = createRequest(UUID.randomUUID(), UUID.randomUUID()); // Different vet ID
            setId(request, requestId);

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
            when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() -> vetApprovalRequestService.withdrawRequest(vetUserId, requestId))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("getPendingRequestsForRescueOrg")
    class GetPendingRequestsForRescueOrg {

        @Test
        @DisplayName("given rescue org with pending requests, when getPendingRequestsForRescueOrg, then returns requests")
        void givenRescueOrgWithPendingRequests_whenGetPendingRequestsForRescueOrg_thenReturnsRequests() {
            // Given
            UUID rescueOrgUserId = UUID.randomUUID();
            RescueOrganization rescueOrg = createRescueOrg(UUID.randomUUID(), true);
            setUserId(rescueOrg, rescueOrgUserId);
            VetApprovalRequest request1 = createRequest(UUID.randomUUID(), rescueOrg.getId());
            VetApprovalRequest request2 = createRequest(UUID.randomUUID(), rescueOrg.getId());

            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(rescueOrg));
            when(requestRepository.findPendingByRescueOrgIdOrderByRequestedAt(rescueOrg.getId()))
                    .thenReturn(List.of(request1, request2));

            // When
            List<VetApprovalRequest> result = vetApprovalRequestService.getPendingRequestsForRescueOrg(rescueOrgUserId);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getRequestsForVet")
    class GetRequestsForVet {

        @Test
        @DisplayName("given vet with requests, when getRequestsForVet, then returns all requests")
        void givenVetWithRequests_whenGetRequestsForVet_thenReturnsAllRequests() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            Vet vet = createVet(vetUserId);
            VetApprovalRequest request = createRequest(vet.getId(), UUID.randomUUID());

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
            when(requestRepository.findByVetId(vet.getId())).thenReturn(List.of(request));

            // When
            List<VetApprovalRequest> result = vetApprovalRequestService.getRequestsForVet(vetUserId);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getAvailableRescueOrgsForVet")
    class GetAvailableRescueOrgsForVet {

        @Test
        @DisplayName("given vet without approvals, when getAvailableRescueOrgsForVet, then returns all verified orgs")
        void givenVetWithoutApprovals_whenGetAvailableRescueOrgsForVet_thenReturnsAllVerifiedOrgs() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            Vet vet = createVet(vetUserId);
            RescueOrganization org1 = createRescueOrg(UUID.randomUUID(), true);
            RescueOrganization org2 = createRescueOrg(UUID.randomUUID(), true);

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
            when(rescueOrganizationRepository.findAllVerified()).thenReturn(List.of(org1, org2));
            when(approvalRepository.existsByVetIdAndRescueOrgId(eq(vet.getId()), any())).thenReturn(false);
            when(requestRepository.existsByVetIdAndRescueOrgIdAndStatus(eq(vet.getId()), any(), eq(VetApprovalRequestStatus.PENDING))).thenReturn(false);

            // When
            List<RescueOrganization> result = vetApprovalRequestService.getAvailableRescueOrgsForVet(vetUserId);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("given vet with existing approval, when getAvailableRescueOrgsForVet, then excludes approved org")
        void givenVetWithExistingApproval_whenGetAvailableRescueOrgsForVet_thenExcludesApprovedOrg() {
            // Given
            UUID vetUserId = UUID.randomUUID();
            Vet vet = createVet(vetUserId);
            UUID approvedOrgId = UUID.randomUUID();
            RescueOrganization approvedOrg = createRescueOrg(approvedOrgId, true);
            RescueOrganization availableOrg = createRescueOrg(UUID.randomUUID(), true);

            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
            when(rescueOrganizationRepository.findAllVerified()).thenReturn(List.of(approvedOrg, availableOrg));
            when(approvalRepository.existsByVetIdAndRescueOrgId(vet.getId(), approvedOrgId)).thenReturn(true);
            when(approvalRepository.existsByVetIdAndRescueOrgId(vet.getId(), availableOrg.getId())).thenReturn(false);
            when(requestRepository.existsByVetIdAndRescueOrgIdAndStatus(eq(vet.getId()), any(), eq(VetApprovalRequestStatus.PENDING))).thenReturn(false);

            // When
            List<RescueOrganization> result = vetApprovalRequestService.getAvailableRescueOrgsForVet(vetUserId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(availableOrg.getId());
        }
    }

    @Nested
    @DisplayName("countPendingRequestsForRescueOrg")
    class CountPendingRequestsForRescueOrg {

        @Test
        @DisplayName("given rescue org with pending requests, when countPendingRequestsForRescueOrg, then returns count")
        void givenRescueOrgWithPendingRequests_whenCountPendingRequestsForRescueOrg_thenReturnsCount() {
            // Given
            UUID rescueOrgUserId = UUID.randomUUID();
            RescueOrganization rescueOrg = createRescueOrg(UUID.randomUUID(), true);
            setUserId(rescueOrg, rescueOrgUserId);

            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(rescueOrg));
            when(requestRepository.countPendingByRescueOrgId(rescueOrg.getId())).thenReturn(5L);

            // When
            long result = vetApprovalRequestService.countPendingRequestsForRescueOrg(rescueOrgUserId);

            // Then
            assertThat(result).isEqualTo(5L);
        }
    }

    // Helper methods
    private Vet createVet(UUID userId) {
        Vet vet = Vet.create(userId, "Happy Paws Clinic", "VET123", "555-1234", "https://happypaws.com", "Description", null);
        setId(vet, UUID.randomUUID());
        return vet;
    }

    private RescueOrganization createRescueOrg(UUID id, boolean verified) {
        RescueOrganization org = RescueOrganization.create(
                UUID.randomUUID(), "Rescue Org", "555-5678", "https://rescueorg.com",
                "Description", "John", "john@example.com", null, null
        );
        setId(org, id);
        if (verified) {
            org.verify();
        }
        return org;
    }

    private VetApprovalRequest createRequest(UUID vetId, UUID rescueOrgId) {
        return VetApprovalRequest.create(vetId, rescueOrgId, "Test message");
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

    private void setUserId(RescueOrganization org, UUID userId) {
        try {
            Field userIdField = RescueOrganization.class.getDeclaredField("userId");
            userIdField.setAccessible(true);
            userIdField.set(org, userId);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set userId", e);
        }
    }
}
