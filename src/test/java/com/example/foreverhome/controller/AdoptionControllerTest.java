package com.example.foreverhome.controller;

import com.example.foreverhome.domain.adoption.Adoption;
import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.domain.adoption.ApplicationStatus;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.adoption.FinalizeAdoptionRequest;
import com.example.foreverhome.dto.adoption.RejectApplicationRequest;
import com.example.foreverhome.dto.adoption.SubmitApplicationRequest;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.AdoptionService;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdoptionController")
class AdoptionControllerTest {

    @Mock
    private AdoptionService adoptionService;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @InjectMocks
    private AdoptionController adoptionController;

    private UUID userId;
    private UUID petId;
    private UUID rescueOrgId;
    private UUID applicationId;
    private UserPrincipal adopterPrincipal;
    private UserPrincipal rescuePrincipal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        petId = UUID.randomUUID();
        rescueOrgId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        adopterPrincipal = new UserPrincipal(userId, UserRole.ADOPTER, true);
        rescuePrincipal = new UserPrincipal(userId, UserRole.RESCUE_ORG, true);
    }

    @Nested
    @DisplayName("POST /api/applications")
    class SubmitApplication {

        @Test
        @DisplayName("should submit application with valid request")
        void shouldSubmitApplicationWithValidRequest() {
            SubmitApplicationRequest request = new SubmitApplicationRequest(petId, "I would love to adopt this pet");
            AdoptionApplication application = mock(AdoptionApplication.class);

            when(adoptionService.submitApplication(userId, petId, "I would love to adopt this pet"))
                    .thenReturn(application);

            ResponseEntity<AdoptionApplication> response = adoptionController.submitApplication(adopterPrincipal, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(application);
        }
    }

    @Nested
    @DisplayName("GET /api/applications")
    class GetMyApplications {

        @Test
        @DisplayName("should return applications for adopter")
        void shouldReturnApplicationsForAdopter() {
            AdoptionApplication application = mock(AdoptionApplication.class);
            List<AdoptionApplication> applications = List.of(application);
            when(adoptionService.getApplicationsForAdopter(userId)).thenReturn(applications);

            ResponseEntity<List<AdoptionApplication>> response = adoptionController.getMyApplications(adopterPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("GET /api/applications/check/{petId}")
    class CheckApplicationStatus {

        @Test
        @DisplayName("should return application status check")
        void shouldReturnApplicationStatusCheck() {
            when(adoptionService.hasActiveApplicationForPet(userId, petId)).thenReturn(true);
            when(adoptionService.getActiveApplicationCount(userId)).thenReturn(2);

            ResponseEntity<AdoptionController.ApplicationCheckResponse> response =
                    adoptionController.checkApplicationStatus(petId, adopterPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().hasApplied()).isTrue();
            assertThat(response.getBody().activeApplicationCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return false when no active application")
        void shouldReturnFalseWhenNoActiveApplication() {
            when(adoptionService.hasActiveApplicationForPet(userId, petId)).thenReturn(false);
            when(adoptionService.getActiveApplicationCount(userId)).thenReturn(0);

            ResponseEntity<AdoptionController.ApplicationCheckResponse> response =
                    adoptionController.checkApplicationStatus(petId, adopterPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().hasApplied()).isFalse();
            assertThat(response.getBody().activeApplicationCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GET /api/pets/{petId}/applications")
    class GetApplicationsForPet {

        @Test
        @DisplayName("should return applications for pet")
        void shouldReturnApplicationsForPet() {
            AdoptionApplication application = mock(AdoptionApplication.class);
            List<AdoptionApplication> applications = List.of(application);
            when(adoptionService.getApplicationsForPet(petId)).thenReturn(applications);

            ResponseEntity<List<AdoptionApplication>> response = adoptionController.getApplicationsForPet(petId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("PUT /api/applications/{id}/approve")
    class ApproveApplication {

        @Test
        @DisplayName("should approve application")
        void shouldApproveApplication() {
            RescueOrganization rescueOrg = mock(RescueOrganization.class);
            when(rescueOrg.getId()).thenReturn(rescueOrgId);
            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));

            AdoptionApplication approvedApplication = mock(AdoptionApplication.class);
            when(approvedApplication.getStatus()).thenReturn(ApplicationStatus.APPROVED);
            when(adoptionService.approveApplication(applicationId, rescueOrgId)).thenReturn(approvedApplication);

            ResponseEntity<AdoptionApplication> response = adoptionController.approveApplication(applicationId, rescuePrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        }

        @Test
        @DisplayName("should throw exception when rescue org not found")
        void shouldThrowExceptionWhenRescueOrgNotFound() {
            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adoptionController.approveApplication(applicationId, rescuePrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rescue organization profile not found");
        }
    }

    @Nested
    @DisplayName("PUT /api/applications/{id}/reject")
    class RejectApplication {

        @Test
        @DisplayName("should reject application with reason")
        void shouldRejectApplicationWithReason() {
            RescueOrganization rescueOrg = mock(RescueOrganization.class);
            when(rescueOrg.getId()).thenReturn(rescueOrgId);
            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));

            RejectApplicationRequest request = new RejectApplicationRequest("Does not meet our criteria");
            AdoptionApplication rejectedApplication = mock(AdoptionApplication.class);
            when(rejectedApplication.getStatus()).thenReturn(ApplicationStatus.REJECTED);
            when(adoptionService.rejectApplication(applicationId, rescueOrgId, "Does not meet our criteria"))
                    .thenReturn(rejectedApplication);

            ResponseEntity<AdoptionApplication> response =
                    adoptionController.rejectApplication(applicationId, rescuePrincipal, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("DELETE /api/applications/{id}")
    class WithdrawApplication {

        @Test
        @DisplayName("should withdraw application")
        void shouldWithdrawApplication() {
            doNothing().when(adoptionService).withdrawApplication(applicationId, userId);

            ResponseEntity<Void> response = adoptionController.withdrawApplication(applicationId, adopterPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(adoptionService).withdrawApplication(applicationId, userId);
        }
    }

    @Nested
    @DisplayName("POST /api/adoptions")
    class FinalizeAdoption {

        @Test
        @DisplayName("should finalize adoption")
        void shouldFinalizeAdoption() {
            RescueOrganization rescueOrg = mock(RescueOrganization.class);
            when(rescueOrg.getId()).thenReturn(rescueOrgId);
            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));

            FinalizeAdoptionRequest request = new FinalizeAdoptionRequest(applicationId);
            Adoption adoption = mock(Adoption.class);
            when(adoptionService.finalizeAdoption(applicationId, rescueOrgId)).thenReturn(adoption);

            ResponseEntity<Adoption> response = adoptionController.finalizeAdoption(rescuePrincipal, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Nested
    @DisplayName("GET /api/adoptions")
    class GetMyAdoptions {

        @Test
        @DisplayName("should return adoptions for adopter")
        void shouldReturnAdoptionsForAdopter() {
            Adoption adoption = mock(Adoption.class);
            List<Adoption> adoptions = List.of(adoption);
            when(adoptionService.getAdoptionsForAdopter(userId)).thenReturn(adoptions);

            ResponseEntity<List<Adoption>> response = adoptionController.getMyAdoptions(adopterPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }
}
