package com.example.foreverhome.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

/**
 * Service for recording custom business metrics.
 * Provides methods to track adoptions, applications, registrations, and other key events.
 */
@Service
public class MetricsService {

    private final Counter adoptionsCompleted;
    private final Counter applicationsSubmitted;
    private final Counter applicationsApproved;
    private final Counter applicationsRejected;
    private final Counter userRegistrations;
    private final Counter petRegistrations;
    private final Counter vetSignOffs;
    private final Counter emailsSent;
    private final Counter emailsFailed;
    private final Counter rateLimitHits;
    private final Timer applicationProcessingTimer;
    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
        this.adoptionsCompleted = registry.counter("foreverhome.adoptions.completed");
        this.applicationsSubmitted = registry.counter("foreverhome.applications.submitted");
        this.applicationsApproved = registry.counter("foreverhome.applications.approved");
        this.applicationsRejected = registry.counter("foreverhome.applications.rejected");
        this.userRegistrations = registry.counter("foreverhome.users.registrations");
        this.petRegistrations = registry.counter("foreverhome.pets.registrations");
        this.vetSignOffs = registry.counter("foreverhome.vet.signoffs");
        this.emailsSent = registry.counter("foreverhome.emails.sent");
        this.emailsFailed = registry.counter("foreverhome.emails.failed");
        this.rateLimitHits = registry.counter("foreverhome.ratelimit.hits");
        this.applicationProcessingTimer = registry.timer("foreverhome.application.processing.time");
    }

    public void recordAdoptionCompleted() {
        adoptionsCompleted.increment();
    }

    public void recordApplicationSubmitted() {
        applicationsSubmitted.increment();
    }

    public void recordApplicationApproved() {
        applicationsApproved.increment();
    }

    public void recordApplicationRejected() {
        applicationsRejected.increment();
    }

    public void recordUserRegistration(String role) {
        userRegistrations.increment();
        registry.counter("foreverhome.users.registrations", "role", role).increment();
    }

    public void recordPetRegistration(String species) {
        petRegistrations.increment();
        registry.counter("foreverhome.pets.registrations", "species", species).increment();
    }

    public void recordVetSignOff(boolean approved) {
        vetSignOffs.increment();
        String result = approved ? "approved" : "declined";
        registry.counter("foreverhome.vet.signoffs", "result", result).increment();
    }

    public void recordEmailSent(String type) {
        emailsSent.increment();
        registry.counter("foreverhome.emails.sent", "type", type).increment();
    }

    public void recordEmailFailed(String type) {
        emailsFailed.increment();
        registry.counter("foreverhome.emails.failed", "type", type).increment();
    }

    public void recordRateLimitHit(String endpoint, String ip) {
        rateLimitHits.increment();
        registry.counter("foreverhome.ratelimit.hits", "endpoint", endpoint).increment();
    }

    public Timer.Sample startApplicationProcessingTimer() {
        return Timer.start(registry);
    }

    public void recordApplicationProcessingTime(Timer.Sample sample) {
        sample.stop(applicationProcessingTimer);
    }

    public void recordPetStatusChange(String fromStatus, String toStatus) {
        registry.counter("foreverhome.pets.status.transitions",
                "from", fromStatus,
                "to", toStatus).increment();
    }

    public void recordLoginAttempt(boolean success) {
        String result = success ? "success" : "failure";
        registry.counter("foreverhome.auth.login.attempts", "result", result).increment();
    }

    // Content moderation metrics

    public void recordFlagCreated(String reason) {
        registry.counter("foreverhome.flags.created", "reason", reason).increment();
    }

    public void recordFlagResolved(boolean approved) {
        String resolution = approved ? "approved" : "dismissed";
        registry.counter("foreverhome.flags.resolved", "resolution", resolution).increment();
    }

    // Admin action metrics

    public void recordAdminAction(String action) {
        registry.counter("foreverhome.admin.actions", "action", action).increment();
    }
}
