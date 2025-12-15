package com.example.foreverhome.controller;

import com.example.foreverhome.controller.ProfileController.*;
import com.example.foreverhome.domain.profile.*;
import com.example.foreverhome.domain.user.NotificationPreferences;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.*;
import com.example.foreverhome.security.UserPrincipal;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController")
class ProfileControllerTest {

    @Mock
    private FosterRepository fosterRepository;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private VetRepository vetRepository;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileController profileController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("GET /api/profile/status")
    class GetProfileStatus {

        @Test
        @DisplayName("given user with complete profile, when getProfileStatus, then returns complete status")
        void givenUserWithCompleteProfile_whenGetProfileStatus_thenReturnsCompleteStatus() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.ADOPTER, true);
            User user = createUser(userId, UserRole.ADOPTER, true);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            ResponseEntity<ProfileStatusResponse> response = profileController.getProfileStatus(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isComplete()).isTrue();
            assertThat(response.getBody().role()).isEqualTo("ADOPTER");
        }

        @Test
        @DisplayName("given user with incomplete profile, when getProfileStatus, then returns incomplete status")
        void givenUserWithIncompleteProfile_whenGetProfileStatus_thenReturnsIncompleteStatus() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.FOSTER, false);
            User user = createUser(userId, UserRole.FOSTER, false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            ResponseEntity<ProfileStatusResponse> response = profileController.getProfileStatus(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isComplete()).isFalse();
        }

        @Test
        @DisplayName("given user not found, when getProfileStatus, then throws ResourceNotFoundException")
        void givenUserNotFound_whenGetProfileStatus_thenThrowsResourceNotFoundException() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.ADOPTER, true);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> profileController.getProfileStatus(principal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GET /api/profile/foster")
    class GetFosterProfile {

        @Test
        @DisplayName("given foster with profile, when getFosterProfile, then returns profile")
        void givenFosterWithProfile_whenGetFosterProfile_thenReturnsProfile() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.FOSTER, true);
            Foster foster = Foster.create(userId, "John", "Doe", "555-1234", null);

            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));

            // When
            ResponseEntity<FosterProfileResponse> response = profileController.getFosterProfile(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().firstName()).isEqualTo("John");
            assertThat(response.getBody().lastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("given foster without profile, when getFosterProfile, then returns empty profile")
        void givenFosterWithoutProfile_whenGetFosterProfile_thenReturnsEmptyProfile() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.FOSTER, false);
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When
            ResponseEntity<FosterProfileResponse> response = profileController.getFosterProfile(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().firstName()).isNull();
            assertThat(response.getBody().isComplete()).isFalse();
        }
    }

    @Nested
    @DisplayName("POST /api/profile/foster")
    class CreateOrUpdateFosterProfile {

        @Test
        @DisplayName("given new foster, when createOrUpdateFosterProfile, then creates profile")
        void givenNewFoster_whenCreateOrUpdateFosterProfile_thenCreatesProfile() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.FOSTER, false);
            FosterProfileRequest request = new FosterProfileRequest("John", "Doe", "555-1234", null);
            User user = createUser(userId, UserRole.FOSTER, false);

            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(fosterRepository.save(any(Foster.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<FosterProfileResponse> response = profileController.createOrUpdateFosterProfile(principal, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().firstName()).isEqualTo("John");
            verify(fosterRepository).save(any(Foster.class));
        }

        @Test
        @DisplayName("given existing foster, when createOrUpdateFosterProfile, then updates profile")
        void givenExistingFoster_whenCreateOrUpdateFosterProfile_thenUpdatesProfile() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.FOSTER, true);
            FosterProfileRequest request = new FosterProfileRequest("Jane", "Smith", "555-9999", null);
            Foster existingFoster = Foster.create(userId, "John", "Doe", "555-1234", null);
            User user = createUser(userId, UserRole.FOSTER, true);

            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(existingFoster));
            when(fosterRepository.save(any(Foster.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<FosterProfileResponse> response = profileController.createOrUpdateFosterProfile(principal, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().firstName()).isEqualTo("Jane");
        }
    }

    @Nested
    @DisplayName("GET /api/profile/adopter")
    class GetAdopterProfile {

        @Test
        @DisplayName("given adopter with profile, when getAdopterProfile, then returns profile")
        void givenAdopterWithProfile_whenGetAdopterProfile_thenReturnsProfile() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.ADOPTER, true);
            Adopter adopter = Adopter.create(userId, "Jane", "Smith", "555-5678", "House", "5 years", null);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));

            // When
            ResponseEntity<AdopterProfileResponse> response = profileController.getAdopterProfile(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().firstName()).isEqualTo("Jane");
            assertThat(response.getBody().livingSituation()).isEqualTo("House");
        }
    }

    @Nested
    @DisplayName("POST /api/profile/adopter")
    class CreateOrUpdateAdopterProfile {

        @Test
        @DisplayName("given new adopter, when createOrUpdateAdopterProfile, then creates profile")
        void givenNewAdopter_whenCreateOrUpdateAdopterProfile_thenCreatesProfile() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.ADOPTER, false);
            AdopterProfileRequest request = new AdopterProfileRequest("Jane", "Smith", "555-5678", "House", "5 years", null);
            User user = createUser(userId, UserRole.ADOPTER, false);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(adopterRepository.save(any(Adopter.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<AdopterProfileResponse> response = profileController.createOrUpdateAdopterProfile(principal, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().firstName()).isEqualTo("Jane");
            assertThat(response.getBody().petExperience()).isEqualTo("5 years");
        }
    }

    @Nested
    @DisplayName("GET /api/profile/vet")
    class GetVetProfile {

        @Test
        @DisplayName("given vet with profile, when getVetProfile, then returns profile with verification status")
        void givenVetWithProfile_whenGetVetProfile_thenReturnsProfileWithVerificationStatus() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.VET, true);
            Vet vet = Vet.create(userId, "Happy Paws Clinic", "VET123", "555-1234", "https://happypaws.com", "Description", null);

            when(vetRepository.findByUserId(userId)).thenReturn(Optional.of(vet));

            // When
            ResponseEntity<VetProfileResponse> response = profileController.getVetProfile(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().clinicName()).isEqualTo("Happy Paws Clinic");
            assertThat(response.getBody().licenseNumber()).isEqualTo("VET123");
            assertThat(response.getBody().verified()).isFalse(); // Not verified initially
        }
    }

    @Nested
    @DisplayName("POST /api/profile/vet")
    class CreateOrUpdateVetProfile {

        @Test
        @DisplayName("given new vet, when createOrUpdateVetProfile, then creates profile")
        void givenNewVet_whenCreateOrUpdateVetProfile_thenCreatesProfile() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.VET, false);
            VetProfileRequest request = new VetProfileRequest("Happy Paws Clinic", "VET123", "555-1234", "https://happypaws.com", "Description", null);
            User user = createUser(userId, UserRole.VET, false);

            when(vetRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(vetRepository.save(any(Vet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<VetProfileResponse> response = profileController.createOrUpdateVetProfile(principal, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().clinicName()).isEqualTo("Happy Paws Clinic");
        }
    }

    @Nested
    @DisplayName("GET /api/profile/rescue-org")
    class GetRescueOrgProfile {

        @Test
        @DisplayName("given rescue org with profile, when getRescueOrgProfile, then returns profile")
        void givenRescueOrgWithProfile_whenGetRescueOrgProfile_thenReturnsProfile() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.RESCUE_ORG, true);
            RescueOrganization rescueOrg = RescueOrganization.create(userId, "Rescue Org", "555-5678", "https://rescue.org", "Description", "John", "john@rescue.org", null, null);

            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(rescueOrg));

            // When
            ResponseEntity<RescueOrgProfileResponse> response = profileController.getRescueOrgProfile(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().name()).isEqualTo("Rescue Org");
            assertThat(response.getBody().contactName()).isEqualTo("John");
        }
    }

    @Nested
    @DisplayName("POST /api/profile/rescue-org")
    class CreateOrUpdateRescueOrgProfile {

        @Test
        @DisplayName("given new rescue org, when createOrUpdateRescueOrgProfile, then creates profile")
        void givenNewRescueOrg_whenCreateOrUpdateRescueOrgProfile_thenCreatesProfile() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.RESCUE_ORG, false);
            RescueOrgProfileRequest request = new RescueOrgProfileRequest("Rescue Org", "555-5678", "https://rescue.org", "Description", "John", "john@rescue.org", null, null);
            User user = createUser(userId, UserRole.RESCUE_ORG, false);

            when(rescueOrganizationRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(rescueOrganizationRepository.save(any(RescueOrganization.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<RescueOrgProfileResponse> response = profileController.createOrUpdateRescueOrgProfile(principal, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().name()).isEqualTo("Rescue Org");
        }
    }

    @Nested
    @DisplayName("GET /api/profile/notifications")
    class GetNotificationPreferences {

        @Test
        @DisplayName("given user with notification preferences, when getNotificationPreferences, then returns preferences")
        void givenUserWithNotificationPreferences_whenGetNotificationPreferences_thenReturnsPreferences() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.ADOPTER, true);
            User user = createUser(userId, UserRole.ADOPTER, true);
            NotificationPreferences prefs = new NotificationPreferences(true, true, false, true);
            user.updateNotificationPreferences(prefs);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            ResponseEntity<NotificationPreferencesResponse> response = profileController.getNotificationPreferences(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().emailStatusChanges()).isTrue();
            assertThat(response.getBody().emailFavoriteUpdates()).isFalse();
        }

        @Test
        @DisplayName("given user without preferences, when getNotificationPreferences, then returns defaults")
        void givenUserWithoutPreferences_whenGetNotificationPreferences_thenReturnsDefaults() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.ADOPTER, true);
            User user = createUser(userId, UserRole.ADOPTER, true);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            ResponseEntity<NotificationPreferencesResponse> response = profileController.getNotificationPreferences(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // Defaults should be returned
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Nested
    @DisplayName("PUT /api/profile/notifications")
    class UpdateNotificationPreferences {

        @Test
        @DisplayName("given valid preferences, when updateNotificationPreferences, then updates and returns preferences")
        void givenValidPreferences_whenUpdateNotificationPreferences_thenUpdatesAndReturnsPreferences() {
            // Given
            UserPrincipal principal = new UserPrincipal(userId, UserRole.ADOPTER, true);
            NotificationPreferencesRequest request = new NotificationPreferencesRequest(true, false, true, true);
            User user = createUser(userId, UserRole.ADOPTER, true);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ResponseEntity<NotificationPreferencesResponse> response = profileController.updateNotificationPreferences(principal, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().emailStatusChanges()).isTrue();
            assertThat(response.getBody().emailNewApplications()).isFalse();
            assertThat(response.getBody().emailFavoriteUpdates()).isTrue();

            verify(userRepository).save(any(User.class));
        }
    }

    // Helper methods
    private User createUser(UUID userId, UserRole role, boolean profileComplete) {
        User user = User.create("test@example.com", "hashedPassword", role);
        user.activate();
        if (profileComplete) {
            user.markProfileComplete();
        }
        setId(user, userId);
        return user;
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
