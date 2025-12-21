package com.example.foreverhome.controller;

import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.repository.PetRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Development-only controller for test utilities.
 * Only available when test mode is enabled.
 */
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "app.test-mode.enabled", havingValue = "true")
public class DevController {

    private static final String TEST_PASSWORD = "password123";

    private final PetRepository petRepository;

    public DevController(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @PostMapping("/moderation/reset")
    public ResponseEntity<Map<String, Object>> resetModerationStatuses() {
        int count = petRepository.resetAllModerationStatuses();
        return ResponseEntity.ok(Map.of(
            "petsAffected", count,
            "message", "All pet moderation statuses reset to PENDING"
        ));
    }

    @GetMapping("/test-accounts")
    public ResponseEntity<Map<String, Object>> getTestAccounts() {
        List<TestAccountResponse> accounts = List.of(
            new TestAccountResponse("admin@test.com", "Test Admin", UserRole.ADMIN.name()),
            new TestAccountResponse("foster-r1@test.com", "Test Foster", UserRole.FOSTER.name()),
            new TestAccountResponse("adopter@test.com", "Test Adopter", UserRole.ADOPTER.name()),
            new TestAccountResponse("vet@test.com", "Test Vet", UserRole.VET.name()),
            new TestAccountResponse("rescue1@test.com", "Test Rescue Org", UserRole.RESCUE_ORG.name())
        );

        return ResponseEntity.ok(Map.of(
            "enabled", true,
            "password", TEST_PASSWORD,
            "accounts", accounts
        ));
    }

    private record TestAccountResponse(String email, String name, String role) {}
}
