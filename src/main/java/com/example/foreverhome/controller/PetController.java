package com.example.foreverhome.controller;

import com.example.foreverhome.dto.pet.CreatePetRequest;
import com.example.foreverhome.dto.pet.DeclinePetRequest;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.dto.pet.SubmitForReviewRequest;
import com.example.foreverhome.dto.pet.UpdatePetRequest;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.PetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public ResponseEntity<List<PetDto>> getAvailablePets() {
        return ResponseEntity.ok(petService.getAvailablePets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetDto> getPet(@PathVariable UUID id) {
        return ResponseEntity.ok(petService.getPet(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('FOSTER')")
    public ResponseEntity<PetDto> createPet(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePetRequest request) {
        PetDto pet = petService.createPet(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pet);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FOSTER')")
    public ResponseEntity<PetDto> updatePet(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePetRequest request) {
        PetDto pet = petService.updatePet(id, principal.userId(), request);
        return ResponseEntity.ok(pet);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('FOSTER')")
    public ResponseEntity<PetDto> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitForReviewRequest request) {
        PetDto pet = petService.submitForReview(id, principal.userId(), request.rescueOrgId());
        return ResponseEntity.ok(pet);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<PetDto> acceptByRescue(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        PetDto pet = petService.acceptByRescue(id, principal.userId());
        return ResponseEntity.ok(pet);
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<PetDto> declineByRescue(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeclinePetRequest request) {
        PetDto pet = petService.declineByRescue(id, principal.userId(), request.reason());
        return ResponseEntity.ok(pet);
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('FOSTER')")
    public ResponseEntity<PetDto> withdrawPet(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        PetDto pet = petService.withdrawPet(id, principal.userId());
        return ResponseEntity.ok(pet);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('FOSTER')")
    public ResponseEntity<List<PetDto>> getMyPets(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(petService.getPetsByFoster(principal.userId()));
    }

    @GetMapping("/rescue/{rescueOrgId}")
    @PreAuthorize("hasRole('RESCUE_ORG') or hasRole('ADMIN')")
    public ResponseEntity<List<PetDto>> getPetsByRescueOrg(@PathVariable UUID rescueOrgId) {
        return ResponseEntity.ok(petService.getPetsByRescueOrg(rescueOrgId));
    }

    @GetMapping("/rescue/{rescueOrgId}/pending")
    @PreAuthorize("hasRole('RESCUE_ORG') or hasRole('ADMIN')")
    public ResponseEntity<List<PetDto>> getPendingPets(@PathVariable UUID rescueOrgId) {
        return ResponseEntity.ok(petService.getPendingPetsForRescueOrg(rescueOrgId));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasRole('VET')")
    public ResponseEntity<PetDto> lookupByMicrochip(
            @RequestParam String microchip,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(petService.findByMicrochipForVet(microchip, principal.userId()));
    }
}
