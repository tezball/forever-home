package com.example.foreverhome.controller;

import com.example.foreverhome.domain.adoption.Adoption;
import com.example.foreverhome.domain.adoption.AdoptionApplication;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.AdoptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AdoptionController {

    private final AdoptionService adoptionService;

    public AdoptionController(AdoptionService adoptionService) {
        this.adoptionService = adoptionService;
    }

    @PostMapping("/applications")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<AdoptionApplication> submitApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> request) {
        UUID petId = UUID.fromString((String) request.get("petId"));
        String message = (String) request.get("message");
        AdoptionApplication application = adoptionService.submitApplication(
                principal.userId(), petId, message
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<List<AdoptionApplication>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adoptionService.getApplicationsForAdopter(principal.userId()));
    }

    @GetMapping("/pets/{petId}/applications")
    @PreAuthorize("hasRole('RESCUE_ORG') or hasRole('ADMIN')")
    public ResponseEntity<List<AdoptionApplication>> getApplicationsForPet(@PathVariable UUID petId) {
        return ResponseEntity.ok(adoptionService.getApplicationsForPet(petId));
    }

    @PutMapping("/applications/{id}/approve")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<AdoptionApplication> approveApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AdoptionApplication application = adoptionService.approveApplication(id, principal.userId());
        return ResponseEntity.ok(application);
    }

    @PutMapping("/applications/{id}/reject")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<AdoptionApplication> rejectApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        AdoptionApplication application = adoptionService.rejectApplication(id, principal.userId(), reason);
        return ResponseEntity.ok(application);
    }

    @DeleteMapping("/applications/{id}")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<Void> withdrawApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        adoptionService.withdrawApplication(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/adoptions")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<Adoption> finalizeAdoption(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {
        UUID applicationId = UUID.fromString(request.get("applicationId"));
        Adoption adoption = adoptionService.finalizeAdoption(applicationId, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(adoption);
    }

    @GetMapping("/adoptions")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<List<Adoption>> getMyAdoptions(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adoptionService.getAdoptionsForAdopter(principal.userId()));
    }
}
