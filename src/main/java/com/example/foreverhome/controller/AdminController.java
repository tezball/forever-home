package com.example.foreverhome.controller;

import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.user.AccountStatus;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.AdoptionRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final AdoptionRepository adoptionRepository;

    public AdminController(UserRepository userRepository, PetRepository petRepository, AdoptionRepository adoptionRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.adoptionRepository = adoptionRepository;
    }

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        long totalUsers = userRepository.count();
        long totalPets = petRepository.count();
        long totalAdoptions = adoptionRepository.countAll();

        // Count pending approvals (RESCUE_ORGs with PENDING status - vets are approved by rescues)
        List<User> pendingRescues = userRepository.findByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING);
        long pendingApprovals = pendingRescues.size();

        // User distribution by role
        long adopters = userRepository.countByRole(UserRole.ADOPTER);
        long fosters = userRepository.countByRole(UserRole.FOSTER);
        long rescueOrgs = userRepository.countByRole(UserRole.RESCUE_ORG);
        long vets = userRepository.countByRole(UserRole.VET);
        long admins = userRepository.countByRole(UserRole.ADMIN);

        // Pet statistics by status
        long petsAvailable = petRepository.countByStatus(PetStatus.AVAILABLE);
        long petsPendingRescue = petRepository.countByStatus(PetStatus.PENDING_RESCUE);
        long petsPendingVet = petRepository.countByStatus(PetStatus.PENDING_VET);
        long petsInProgress = petRepository.countByStatus(PetStatus.IN_PROGRESS);
        long petsAdopted = petRepository.countByStatus(PetStatus.ADOPTED);
        long petsDraft = petRepository.countByStatus(PetStatus.DRAFT);
        long petsOnHold = petRepository.countByStatus(PetStatus.ON_HOLD);
        long petsWithdrawn = petRepository.countByStatus(PetStatus.WITHDRAWN);

        var response = new AnalyticsResponse(
                totalUsers,
                totalPets,
                totalAdoptions,
                pendingApprovals,
                new UserDistribution(adopters, fosters, rescueOrgs, vets, admins),
                new PetStatistics(petsAvailable, petsPendingRescue, petsPendingVet, petsInProgress, petsAdopted, petsDraft, petsOnHold, petsWithdrawn)
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/approvals")
    public ResponseEntity<List<ApprovalResponse>> getPendingApprovals() {
        // Only rescue organizations are approved by admins - vets are approved by rescue centers
        List<User> pendingRescues = userRepository.findByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING);

        List<ApprovalResponse> approvals = new java.util.ArrayList<>();

        for (User rescue : pendingRescues) {
            approvals.add(new ApprovalResponse(
                    rescue.getId().toString(),
                    "RESCUE_ORG",
                    rescue.getName() != null ? rescue.getName() : rescue.getEmail(),
                    rescue.getEmail(),
                    rescue.getCreatedAt().toString()
            ));
        }

        return ResponseEntity.ok(approvals);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        List<UserResponse> users = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(u -> new UserResponse(
                        u.getId().toString(),
                        u.getName() != null ? u.getName() : u.getEmail(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.getStatus().name(),
                        u.isProfileComplete()
                ))
                .toList();

        return ResponseEntity.ok(users);
    }

    @PutMapping("/approvals/{type}/{id}/approve")
    public ResponseEntity<Void> approveUser(@PathVariable String type, @PathVariable String id) {
        var userId = java.util.UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.activate();
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/approvals/{type}/{id}/reject")
    public ResponseEntity<Void> rejectUser(@PathVariable String type, @PathVariable String id) {
        var userId = java.util.UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.suspend();
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable String id) {
        var userId = java.util.UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.suspend();
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable String id) {
        var userId = java.util.UUID.fromString(id);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.reactivate();
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    // Response DTOs
    record AnalyticsResponse(
            long totalUsers,
            long totalPets,
            long totalAdoptions,
            long pendingApprovals,
            UserDistribution userDistribution,
            PetStatistics petStatistics
    ) {}

    record UserDistribution(
            long adopters,
            long fosters,
            long rescueOrgs,
            long vets,
            long admins
    ) {}

    record PetStatistics(
            long available,
            long pendingRescue,
            long pendingVet,
            long inProgress,
            long adopted,
            long draft,
            long onHold,
            long withdrawn
    ) {}

    record ApprovalResponse(
            String id,
            String type,
            String name,
            String email,
            String submittedAt
    ) {}

    record UserResponse(
            String id,
            String name,
            String email,
            String role,
            String status,
            boolean profileComplete
    ) {}
}
