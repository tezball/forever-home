package com.example.foreverhome.controller;

import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.domain.verification.VetApproval;
import com.example.foreverhome.domain.verification.VetApprovalRequest;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.VetRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.VetApprovalRequestService;
import com.example.foreverhome.service.VetApprovalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RescueOrgController")
class RescueOrgControllerTest {

    @Mock
    private VetApprovalService vetApprovalService;

    @Mock
    private VetApprovalRequestService vetApprovalRequestService;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @Mock
    private VetRepository vetRepository;

    @InjectMocks
    private RescueOrgController rescueOrgController;

    private UserPrincipal rescueOrgPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        rescueOrgPrincipal = new UserPrincipal(userId, UserRole.RESCUE_ORG, true);
    }

    @Nested
    @DisplayName("GET /api/rescue-org/vets/approved")
    class GetApprovedVets {

        @Test
        @DisplayName("given rescue org with approved vets, when getApprovedVets, then returns list of vets")
        void givenRescueOrgWithApprovedVets_whenGetApprovedVets_thenReturnsListOfVets() {
            // Given
            Vet vet1 = createVet("Happy Paws Clinic", "VET001");
            Vet vet2 = createVet("Animal Care Center", "VET002");
            when(vetApprovalService.getApprovedVetsForRescueOrg(userId)).thenReturn(List.of(vet1, vet2));

            // When
            ResponseEntity<List<RescueOrgController.VetResponse>> response = rescueOrgController.getApprovedVets(rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).clinicName()).isEqualTo("Happy Paws Clinic");
        }

        @Test
        @DisplayName("given rescue org with no approved vets, when getApprovedVets, then returns empty list")
        void givenRescueOrgWithNoApprovedVets_whenGetApprovedVets_thenReturnsEmptyList() {
            // Given
            when(vetApprovalService.getApprovedVetsForRescueOrg(userId)).thenReturn(List.of());

            // When
            ResponseEntity<List<RescueOrgController.VetResponse>> response = rescueOrgController.getApprovedVets(rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/rescue-org/vets/pending")
    class GetPendingVets {

        @Test
        @DisplayName("given vets available for approval, when getPendingVets, then returns list of vets")
        void givenVetsAvailableForApproval_whenGetPendingVets_thenReturnsListOfVets() {
            // Given
            Vet vet = createVet("New Vet Clinic", "VET003");
            when(vetApprovalService.getPendingVetsForRescueOrg(userId)).thenReturn(List.of(vet));

            // When
            ResponseEntity<List<RescueOrgController.VetResponse>> response = rescueOrgController.getPendingVets(rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("POST /api/rescue-org/vets/{vetId}/approve")
    class ApproveVet {

        @Test
        @DisplayName("given valid vet ID, when approveVet, then returns created approval")
        void givenValidVetId_whenApproveVet_thenReturnsCreatedApproval() {
            // Given
            UUID vetId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            VetApproval approval = VetApproval.create(vetId, rescueOrgId, userId);
            setId(approval, UUID.randomUUID());
            when(vetApprovalService.approveVet(userId, vetId)).thenReturn(approval);

            // When
            ResponseEntity<RescueOrgController.VetApprovalResponse> response = rescueOrgController.approveVet(vetId, rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().vetId()).isEqualTo(vetId);
            verify(vetApprovalService).approveVet(userId, vetId);
        }
    }

    @Nested
    @DisplayName("DELETE /api/rescue-org/vets/{vetId}/approve")
    class RevokeVetApproval {

        @Test
        @DisplayName("given approved vet, when revokeVetApproval, then returns no content")
        void givenApprovedVet_whenRevokeVetApproval_thenReturnsNoContent() {
            // Given
            UUID vetId = UUID.randomUUID();
            doNothing().when(vetApprovalService).revokeVetApproval(userId, vetId);

            // When
            ResponseEntity<Void> response = rescueOrgController.revokeVetApproval(vetId, rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(vetApprovalService).revokeVetApproval(userId, vetId);
        }
    }

    @Nested
    @DisplayName("GET /api/rescue-org/approval-requests")
    class GetPendingApprovalRequests {

        @Test
        @DisplayName("given pending requests, when getPendingApprovalRequests, then returns list of requests")
        void givenPendingRequests_whenGetPendingApprovalRequests_thenReturnsListOfRequests() {
            // Given
            UUID vetId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            VetApprovalRequest request = VetApprovalRequest.create(vetId, rescueOrgId, "Please approve me");
            setId(request, UUID.randomUUID());
            Vet vet = createVet("Test Clinic", "VET123");
            setId(vet, vetId);

            when(vetApprovalRequestService.getPendingRequestsForRescueOrg(userId)).thenReturn(List.of(request));
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(vet));

            // When
            ResponseEntity<List<RescueOrgController.VetApprovalRequestResponse>> response =
                    rescueOrgController.getPendingApprovalRequests(rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).vetClinicName()).isEqualTo("Test Clinic");
        }
    }

    @Nested
    @DisplayName("GET /api/rescue-org/approval-requests/count")
    class GetPendingApprovalRequestCount {

        @Test
        @DisplayName("given pending requests, when getPendingApprovalRequestCount, then returns count")
        void givenPendingRequests_whenGetPendingApprovalRequestCount_thenReturnsCount() {
            // Given
            when(vetApprovalRequestService.countPendingRequestsForRescueOrg(userId)).thenReturn(5L);

            // When
            ResponseEntity<RescueOrgController.ApprovalRequestCountResponse> response =
                    rescueOrgController.getPendingApprovalRequestCount(rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().count()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("POST /api/rescue-org/approval-requests/{requestId}/approve")
    class ApproveApprovalRequest {

        @Test
        @DisplayName("given valid request ID, when approveApprovalRequest, then returns created approval")
        void givenValidRequestId_whenApproveApprovalRequest_thenReturnsCreatedApproval() {
            // Given
            UUID requestId = UUID.randomUUID();
            UUID vetId = UUID.randomUUID();
            UUID rescueOrgId = UUID.randomUUID();
            VetApproval approval = VetApproval.create(vetId, rescueOrgId, userId);
            setId(approval, UUID.randomUUID());
            when(vetApprovalRequestService.approveRequest(userId, requestId)).thenReturn(approval);

            // When
            ResponseEntity<RescueOrgController.VetApprovalResponse> response =
                    rescueOrgController.approveApprovalRequest(requestId, rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(vetApprovalRequestService).approveRequest(userId, requestId);
        }
    }

    @Nested
    @DisplayName("POST /api/rescue-org/approval-requests/{requestId}/reject")
    class RejectApprovalRequest {

        @Test
        @DisplayName("given valid request ID with reason, when rejectApprovalRequest, then returns no content")
        void givenValidRequestIdWithReason_whenRejectApprovalRequest_thenReturnsNoContent() {
            // Given
            UUID requestId = UUID.randomUUID();
            RescueOrgController.RejectRequestBody requestBody = new RescueOrgController.RejectRequestBody("Not qualified");
            doNothing().when(vetApprovalRequestService).rejectRequest(userId, requestId, "Not qualified");

            // When
            ResponseEntity<Void> response = rescueOrgController.rejectApprovalRequest(requestId, requestBody, rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(vetApprovalRequestService).rejectRequest(userId, requestId, "Not qualified");
        }

        @Test
        @DisplayName("given valid request ID without reason, when rejectApprovalRequest, then returns no content")
        void givenValidRequestIdWithoutReason_whenRejectApprovalRequest_thenReturnsNoContent() {
            // Given
            UUID requestId = UUID.randomUUID();
            doNothing().when(vetApprovalRequestService).rejectRequest(userId, requestId, null);

            // When
            ResponseEntity<Void> response = rescueOrgController.rejectApprovalRequest(requestId, null, rescueOrgPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(vetApprovalRequestService).rejectRequest(userId, requestId, null);
        }
    }

    // Helper methods
    private Vet createVet(String clinicName, String licenseNumber) {
        Vet vet = Vet.create(UUID.randomUUID(), clinicName, licenseNumber, "555-1234", null, null, null);
        setId(vet, UUID.randomUUID());
        return vet;
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
