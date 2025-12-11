package com.example.foreverhome.service;

import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.verification.VetApproval;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.VetApprovalRepository;
import com.example.foreverhome.repository.VetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VetApprovalService")
class VetApprovalServiceTest {

    @Mock
    private VetApprovalRepository vetApprovalRepository;

    @Mock
    private VetRepository vetRepository;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private VetApprovalService vetApprovalService;

    @Captor
    private ArgumentCaptor<VetApproval> vetApprovalCaptor;

    @Captor
    private ArgumentCaptor<Vet> vetCaptor;

    private UUID rescueOrgUserId;
    private UUID rescueOrgId;
    private UUID vetId;
    private UUID vetUserId;
    private RescueOrganization mockRescueOrg;
    private Vet mockVet;

    @BeforeEach
    void setUp() {
        rescueOrgUserId = UUID.randomUUID();
        rescueOrgId = UUID.randomUUID();
        vetId = UUID.randomUUID();
        vetUserId = UUID.randomUUID();

        mockRescueOrg = mock(RescueOrganization.class);
        when(mockRescueOrg.getId()).thenReturn(rescueOrgId);
        when(mockRescueOrg.getName()).thenReturn("Happy Paws Rescue");
        when(mockRescueOrg.isVerified()).thenReturn(true);

        mockVet = mock(Vet.class);
        when(mockVet.getId()).thenReturn(vetId);
        when(mockVet.getUserId()).thenReturn(vetUserId);
        when(mockVet.isVerified()).thenReturn(false);
    }

    @Nested
    @DisplayName("approveVet")
    class ApproveVet {

        @Test
        @DisplayName("should approve vet successfully")
        void shouldApproveVetSuccessfully() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(mockVet));
            when(vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrgId)).thenReturn(false);
            when(vetApprovalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            VetApproval result = vetApprovalService.approveVet(rescueOrgUserId, vetId);

            assertThat(result).isNotNull();
            verify(vetApprovalRepository).save(vetApprovalCaptor.capture());
            assertThat(vetApprovalCaptor.getValue().getVetId()).isEqualTo(vetId);
            assertThat(vetApprovalCaptor.getValue().getRescueOrgId()).isEqualTo(rescueOrgId);
        }

        @Test
        @DisplayName("should mark vet as verified on first approval")
        void shouldMarkVetAsVerifiedOnFirstApproval() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(mockVet));
            when(vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrgId)).thenReturn(false);
            when(vetApprovalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            vetApprovalService.approveVet(rescueOrgUserId, vetId);

            verify(mockVet).verify();
            verify(vetRepository).save(mockVet);
        }

        @Test
        @DisplayName("should not verify vet if already verified")
        void shouldNotVerifyVetIfAlreadyVerified() {
            when(mockVet.isVerified()).thenReturn(true);
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(mockVet));
            when(vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrgId)).thenReturn(false);
            when(vetApprovalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            vetApprovalService.approveVet(rescueOrgUserId, vetId);

            verify(mockVet, never()).verify();
        }

        @Test
        @DisplayName("should send notification to vet")
        void shouldSendNotificationToVet() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(mockVet));
            when(vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrgId)).thenReturn(false);
            when(vetApprovalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            vetApprovalService.approveVet(rescueOrgUserId, vetId);

            verify(notificationService).notifyVetApproved(vetUserId, "Happy Paws Rescue");
        }

        @Test
        @DisplayName("should return existing approval if already approved")
        void shouldReturnExistingApprovalIfAlreadyApproved() {
            VetApproval existingApproval = mock(VetApproval.class);
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(mockVet));
            when(vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrgId)).thenReturn(true);
            when(vetApprovalRepository.findByVetIdAndRescueOrgId(vetId, rescueOrgId)).thenReturn(Optional.of(existingApproval));

            VetApproval result = vetApprovalService.approveVet(rescueOrgUserId, vetId);

            assertThat(result).isEqualTo(existingApproval);
            verify(vetApprovalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when rescue org not found")
        void shouldThrowExceptionWhenRescueOrgNotFound() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vetApprovalService.approveVet(rescueOrgUserId, vetId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rescue organization not found");
        }

        @Test
        @DisplayName("should throw exception when rescue org not verified")
        void shouldThrowExceptionWhenRescueOrgNotVerified() {
            when(mockRescueOrg.isVerified()).thenReturn(false);
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));

            assertThatThrownBy(() -> vetApprovalService.approveVet(rescueOrgUserId, vetId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("verified before approving vets");
        }

        @Test
        @DisplayName("should throw exception when vet not found")
        void shouldThrowExceptionWhenVetNotFound() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetRepository.findById(vetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vetApprovalService.approveVet(rescueOrgUserId, vetId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("revokeVetApproval")
    class RevokeVetApproval {

        @Test
        @DisplayName("should revoke vet approval")
        void shouldRevokeVetApproval() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetApprovalRepository.countByVetId(vetId)).thenReturn(1L);

            vetApprovalService.revokeVetApproval(rescueOrgUserId, vetId);

            verify(vetApprovalRepository).deleteByVetIdAndRescueOrgId(vetId, rescueOrgId);
        }

        @Test
        @DisplayName("should unverify vet when last approval is revoked")
        void shouldUnverifyVetWhenLastApprovalIsRevoked() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetApprovalRepository.countByVetId(vetId)).thenReturn(0L);
            when(vetRepository.findById(vetId)).thenReturn(Optional.of(mockVet));

            vetApprovalService.revokeVetApproval(rescueOrgUserId, vetId);

            verify(mockVet).unverify();
            verify(vetRepository).save(mockVet);
        }

        @Test
        @DisplayName("should not unverify vet when other approvals remain")
        void shouldNotUnverifyVetWhenOtherApprovalsRemain() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetApprovalRepository.countByVetId(vetId)).thenReturn(2L);

            vetApprovalService.revokeVetApproval(rescueOrgUserId, vetId);

            verify(vetRepository, never()).findById(any());
            verify(vetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when rescue org not found")
        void shouldThrowExceptionWhenRescueOrgNotFound() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vetApprovalService.revokeVetApproval(rescueOrgUserId, vetId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getApprovedVetsForRescueOrg")
    class GetApprovedVetsForRescueOrg {

        @Test
        @DisplayName("should return approved vets")
        void shouldReturnApprovedVets() {
            VetApproval approval = mock(VetApproval.class);
            when(approval.getVetId()).thenReturn(vetId);
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetApprovalRepository.findByRescueOrgId(rescueOrgId)).thenReturn(List.of(approval));
            when(vetRepository.findByIdIn(List.of(vetId))).thenReturn(List.of(mockVet));

            List<Vet> result = vetApprovalService.getApprovedVetsForRescueOrg(rescueOrgUserId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(mockVet);
        }

        @Test
        @DisplayName("should return empty list when no approvals")
        void shouldReturnEmptyListWhenNoApprovals() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetApprovalRepository.findByRescueOrgId(rescueOrgId)).thenReturn(List.of());

            List<Vet> result = vetApprovalService.getApprovedVetsForRescueOrg(rescueOrgUserId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRescueOrgsForVet")
    class GetRescueOrgsForVet {

        @Test
        @DisplayName("should return rescue orgs that approved vet")
        void shouldReturnRescueOrgsThatApprovedVet() {
            VetApproval approval = mock(VetApproval.class);
            when(approval.getRescueOrgId()).thenReturn(rescueOrgId);
            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(mockVet));
            when(vetApprovalRepository.findByVetId(vetId)).thenReturn(List.of(approval));
            when(rescueOrganizationRepository.findById(rescueOrgId)).thenReturn(Optional.of(mockRescueOrg));

            List<RescueOrganization> result = vetApprovalService.getRescueOrgsForVet(vetUserId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(mockRescueOrg);
        }

        @Test
        @DisplayName("should throw exception when vet not found")
        void shouldThrowExceptionWhenVetNotFound() {
            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vetApprovalService.getRescueOrgsForVet(vetUserId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPendingVetsForRescueOrg")
    class GetPendingVetsForRescueOrg {

        @Test
        @DisplayName("should return vets not approved by rescue org")
        void shouldReturnVetsNotApprovedByRescueOrg() {
            when(rescueOrganizationRepository.findByUserId(rescueOrgUserId)).thenReturn(Optional.of(mockRescueOrg));
            when(vetRepository.findNotApprovedByRescueOrg(rescueOrgId)).thenReturn(List.of(mockVet));

            List<Vet> result = vetApprovalService.getPendingVetsForRescueOrg(rescueOrgUserId);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("isVetApprovedByRescueOrg")
    class IsVetApprovedByRescueOrg {

        @Test
        @DisplayName("should return true when vet is approved")
        void shouldReturnTrueWhenVetIsApproved() {
            when(vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrgId)).thenReturn(true);

            boolean result = vetApprovalService.isVetApprovedByRescueOrg(vetId, rescueOrgId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when vet is not approved")
        void shouldReturnFalseWhenVetIsNotApproved() {
            when(vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrgId)).thenReturn(false);

            boolean result = vetApprovalService.isVetApprovedByRescueOrg(vetId, rescueOrgId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isVetUserApprovedByRescueOrg")
    class IsVetUserApprovedByRescueOrg {

        @Test
        @DisplayName("should return true when vet user is approved")
        void shouldReturnTrueWhenVetUserIsApproved() {
            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.of(mockVet));
            when(vetApprovalRepository.existsByVetIdAndRescueOrgId(vetId, rescueOrgId)).thenReturn(true);

            boolean result = vetApprovalService.isVetUserApprovedByRescueOrg(vetUserId, rescueOrgId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when vet user not found")
        void shouldReturnFalseWhenVetUserNotFound() {
            when(vetRepository.findByUserId(vetUserId)).thenReturn(Optional.empty());

            boolean result = vetApprovalService.isVetUserApprovedByRescueOrg(vetUserId, rescueOrgId);

            assertThat(result).isFalse();
        }
    }
}
