package com.example.foreverhome.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MetricsService")
class MetricsServiceTest {

    private MeterRegistry registry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metricsService = new MetricsService(registry);
    }

    @Nested
    @DisplayName("recordAdoptionCompleted")
    class RecordAdoptionCompleted {

        @Test
        @DisplayName("given metrics service, when recordAdoptionCompleted, then increments adoption counter")
        void givenMetricsService_whenRecordAdoptionCompleted_thenIncrementsAdoptionCounter() {
            // Given
            double initialCount = getCounterValue("foreverhome.adoptions.completed");

            // When
            metricsService.recordAdoptionCompleted();

            // Then
            double newCount = getCounterValue("foreverhome.adoptions.completed");
            assertThat(newCount).isEqualTo(initialCount + 1);
        }

        @Test
        @DisplayName("given multiple calls, when recordAdoptionCompleted, then increments correctly")
        void givenMultipleCalls_whenRecordAdoptionCompleted_thenIncrementsCorrectly() {
            // Given
            double initialCount = getCounterValue("foreverhome.adoptions.completed");

            // When
            metricsService.recordAdoptionCompleted();
            metricsService.recordAdoptionCompleted();
            metricsService.recordAdoptionCompleted();

            // Then
            double newCount = getCounterValue("foreverhome.adoptions.completed");
            assertThat(newCount).isEqualTo(initialCount + 3);
        }
    }

    @Nested
    @DisplayName("recordApplicationSubmitted")
    class RecordApplicationSubmitted {

        @Test
        @DisplayName("given metrics service, when recordApplicationSubmitted, then increments counter")
        void givenMetricsService_whenRecordApplicationSubmitted_thenIncrementsCounter() {
            // Given
            double initialCount = getCounterValue("foreverhome.applications.submitted");

            // When
            metricsService.recordApplicationSubmitted();

            // Then
            double newCount = getCounterValue("foreverhome.applications.submitted");
            assertThat(newCount).isEqualTo(initialCount + 1);
        }
    }

    @Nested
    @DisplayName("recordApplicationApproved")
    class RecordApplicationApproved {

        @Test
        @DisplayName("given metrics service, when recordApplicationApproved, then increments counter")
        void givenMetricsService_whenRecordApplicationApproved_thenIncrementsCounter() {
            // Given
            double initialCount = getCounterValue("foreverhome.applications.approved");

            // When
            metricsService.recordApplicationApproved();

            // Then
            double newCount = getCounterValue("foreverhome.applications.approved");
            assertThat(newCount).isEqualTo(initialCount + 1);
        }
    }

    @Nested
    @DisplayName("recordApplicationRejected")
    class RecordApplicationRejected {

        @Test
        @DisplayName("given metrics service, when recordApplicationRejected, then increments counter")
        void givenMetricsService_whenRecordApplicationRejected_thenIncrementsCounter() {
            // Given
            double initialCount = getCounterValue("foreverhome.applications.rejected");

            // When
            metricsService.recordApplicationRejected();

            // Then
            double newCount = getCounterValue("foreverhome.applications.rejected");
            assertThat(newCount).isEqualTo(initialCount + 1);
        }
    }

    @Nested
    @DisplayName("recordUserRegistration")
    class RecordUserRegistration {

        @Test
        @DisplayName("given role, when recordUserRegistration, then increments total and role-specific counters")
        void givenRole_whenRecordUserRegistration_thenIncrementsTotalAndRoleSpecificCounters() {
            // Given
            String role = "ADOPTER";
            double initialTotal = getCounterValue("foreverhome.users.registrations.total");

            // When
            metricsService.recordUserRegistration(role);

            // Then
            double newTotal = getCounterValue("foreverhome.users.registrations.total");
            assertThat(newTotal).isEqualTo(initialTotal + 1);

            Counter roleCounter = registry.find("foreverhome.users.registrations").tag("role", role).counter();
            assertThat(roleCounter).isNotNull();
            assertThat(roleCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("given different roles, when recordUserRegistration, then tracks separately")
        void givenDifferentRoles_whenRecordUserRegistration_thenTracksSeparately() {
            // When
            metricsService.recordUserRegistration("ADOPTER");
            metricsService.recordUserRegistration("ADOPTER");
            metricsService.recordUserRegistration("FOSTER");

            // Then
            Counter adopterCounter = registry.find("foreverhome.users.registrations").tag("role", "ADOPTER").counter();
            Counter fosterCounter = registry.find("foreverhome.users.registrations").tag("role", "FOSTER").counter();

            assertThat(adopterCounter.count()).isEqualTo(2.0);
            assertThat(fosterCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordPetRegistration")
    class RecordPetRegistration {

        @Test
        @DisplayName("given species, when recordPetRegistration, then increments total and species-specific counters")
        void givenSpecies_whenRecordPetRegistration_thenIncrementsTotalAndSpeciesSpecificCounters() {
            // Given
            String species = "DOG";
            double initialTotal = getCounterValue("foreverhome.pets.registrations");

            // When
            metricsService.recordPetRegistration(species);

            // Then
            double newTotal = getCounterValue("foreverhome.pets.registrations");
            assertThat(newTotal).isEqualTo(initialTotal + 1);

            Counter speciesCounter = registry.find("foreverhome.pets.registrations").tag("species", species).counter();
            assertThat(speciesCounter).isNotNull();
            assertThat(speciesCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordVetSignOff")
    class RecordVetSignOff {

        @Test
        @DisplayName("given approved true, when recordVetSignOff, then increments approved counter")
        void givenApprovedTrue_whenRecordVetSignOff_thenIncrementsApprovedCounter() {
            // Given
            double initialTotal = getCounterValue("foreverhome.vet.signoffs");

            // When
            metricsService.recordVetSignOff(true);

            // Then
            double newTotal = getCounterValue("foreverhome.vet.signoffs");
            assertThat(newTotal).isEqualTo(initialTotal + 1);

            Counter approvedCounter = registry.find("foreverhome.vet.signoffs").tag("result", "approved").counter();
            assertThat(approvedCounter).isNotNull();
            assertThat(approvedCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("given approved false, when recordVetSignOff, then increments declined counter")
        void givenApprovedFalse_whenRecordVetSignOff_thenIncrementsDeclinedCounter() {
            // When
            metricsService.recordVetSignOff(false);

            // Then
            Counter declinedCounter = registry.find("foreverhome.vet.signoffs").tag("result", "declined").counter();
            assertThat(declinedCounter).isNotNull();
            assertThat(declinedCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordEmailSent")
    class RecordEmailSent {

        @Test
        @DisplayName("given email type, when recordEmailSent, then increments total and type-specific counters")
        void givenEmailType_whenRecordEmailSent_thenIncrementsTotalAndTypeSpecificCounters() {
            // Given
            String type = "verification";
            double initialTotal = getCounterValue("foreverhome.emails.sent");

            // When
            metricsService.recordEmailSent(type);

            // Then
            double newTotal = getCounterValue("foreverhome.emails.sent");
            assertThat(newTotal).isEqualTo(initialTotal + 1);

            Counter typeCounter = registry.find("foreverhome.emails.sent").tag("type", type).counter();
            assertThat(typeCounter).isNotNull();
            assertThat(typeCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordEmailFailed")
    class RecordEmailFailed {

        @Test
        @DisplayName("given email type, when recordEmailFailed, then increments failed counter")
        void givenEmailType_whenRecordEmailFailed_thenIncrementsFailedCounter() {
            // Given
            String type = "verification";
            double initialTotal = getCounterValue("foreverhome.emails.failed");

            // When
            metricsService.recordEmailFailed(type);

            // Then
            double newTotal = getCounterValue("foreverhome.emails.failed");
            assertThat(newTotal).isEqualTo(initialTotal + 1);
        }
    }

    @Nested
    @DisplayName("recordRateLimitHit")
    class RecordRateLimitHit {

        @Test
        @DisplayName("given endpoint and IP, when recordRateLimitHit, then increments counter")
        void givenEndpointAndIp_whenRecordRateLimitHit_thenIncrementsCounter() {
            // Given
            String endpoint = "/api/auth/login";
            String ip = "192.168.1.1";
            double initialTotal = getCounterValue("foreverhome.ratelimit.hits");

            // When
            metricsService.recordRateLimitHit(endpoint, ip);

            // Then
            double newTotal = getCounterValue("foreverhome.ratelimit.hits");
            assertThat(newTotal).isEqualTo(initialTotal + 1);

            Counter endpointCounter = registry.find("foreverhome.ratelimit.hits").tag("endpoint", endpoint).counter();
            assertThat(endpointCounter).isNotNull();
            assertThat(endpointCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("applicationProcessingTimer")
    class ApplicationProcessingTimer {

        @Test
        @DisplayName("given timer started, when recordApplicationProcessingTime, then records duration")
        void givenTimerStarted_whenRecordApplicationProcessingTime_thenRecordsDuration() {
            // Given
            Timer.Sample sample = metricsService.startApplicationProcessingTimer();

            // When
            metricsService.recordApplicationProcessingTime(sample);

            // Then
            Timer timer = registry.find("foreverhome.application.processing.time").timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("recordPetStatusChange")
    class RecordPetStatusChange {

        @Test
        @DisplayName("given status transition, when recordPetStatusChange, then increments transition counter")
        void givenStatusTransition_whenRecordPetStatusChange_thenIncrementsTransitionCounter() {
            // When
            metricsService.recordPetStatusChange("PENDING_RESCUE", "AVAILABLE");

            // Then
            Counter transitionCounter = registry.find("foreverhome.pets.status.transitions")
                    .tag("from", "PENDING_RESCUE")
                    .tag("to", "AVAILABLE")
                    .counter();
            assertThat(transitionCounter).isNotNull();
            assertThat(transitionCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordLoginAttempt")
    class RecordLoginAttempt {

        @Test
        @DisplayName("given successful login, when recordLoginAttempt, then increments success counter")
        void givenSuccessfulLogin_whenRecordLoginAttempt_thenIncrementsSuccessCounter() {
            // When
            metricsService.recordLoginAttempt(true);

            // Then
            Counter successCounter = registry.find("foreverhome.auth.login.attempts")
                    .tag("result", "success")
                    .counter();
            assertThat(successCounter).isNotNull();
            assertThat(successCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("given failed login, when recordLoginAttempt, then increments failure counter")
        void givenFailedLogin_whenRecordLoginAttempt_thenIncrementsFailureCounter() {
            // When
            metricsService.recordLoginAttempt(false);

            // Then
            Counter failureCounter = registry.find("foreverhome.auth.login.attempts")
                    .tag("result", "failure")
                    .counter();
            assertThat(failureCounter).isNotNull();
            assertThat(failureCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordFlagCreated")
    class RecordFlagCreated {

        @Test
        @DisplayName("given reason, when recordFlagCreated, then increments flag counter")
        void givenReason_whenRecordFlagCreated_thenIncrementsFlagCounter() {
            // When
            metricsService.recordFlagCreated("INAPPROPRIATE_CONTENT");

            // Then
            Counter flagCounter = registry.find("foreverhome.flags.created")
                    .tag("reason", "INAPPROPRIATE_CONTENT")
                    .counter();
            assertThat(flagCounter).isNotNull();
            assertThat(flagCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordFlagResolved")
    class RecordFlagResolved {

        @Test
        @DisplayName("given flag approved, when recordFlagResolved, then increments approved counter")
        void givenFlagApproved_whenRecordFlagResolved_thenIncrementsApprovedCounter() {
            // When
            metricsService.recordFlagResolved(true);

            // Then
            Counter approvedCounter = registry.find("foreverhome.flags.resolved")
                    .tag("resolution", "approved")
                    .counter();
            assertThat(approvedCounter).isNotNull();
            assertThat(approvedCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("given flag dismissed, when recordFlagResolved, then increments dismissed counter")
        void givenFlagDismissed_whenRecordFlagResolved_thenIncrementsDismissedCounter() {
            // When
            metricsService.recordFlagResolved(false);

            // Then
            Counter dismissedCounter = registry.find("foreverhome.flags.resolved")
                    .tag("resolution", "dismissed")
                    .counter();
            assertThat(dismissedCounter).isNotNull();
            assertThat(dismissedCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordAdminAction")
    class RecordAdminAction {

        @Test
        @DisplayName("given admin action, when recordAdminAction, then increments action counter")
        void givenAdminAction_whenRecordAdminAction_thenIncrementsActionCounter() {
            // When
            metricsService.recordAdminAction("USER_SUSPENDED");

            // Then
            Counter actionCounter = registry.find("foreverhome.admin.actions")
                    .tag("action", "USER_SUSPENDED")
                    .counter();
            assertThat(actionCounter).isNotNull();
            assertThat(actionCounter.count()).isEqualTo(1.0);
        }
    }

    // Helper method
    private double getCounterValue(String name) {
        Counter counter = registry.find(name).counter();
        return counter != null ? counter.count() : 0.0;
    }
}
