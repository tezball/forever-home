package com.example.foreverhome.controller;

import com.example.foreverhome.domain.pet.PetStatusHistory;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.dto.PagedResponse;
import com.example.foreverhome.dto.pet.CreatePetRequest;
import com.example.foreverhome.dto.pet.DeclinePetRequest;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.dto.pet.PetStatusHistoryDto;
import com.example.foreverhome.dto.pet.SubmitForReviewRequest;
import com.example.foreverhome.dto.pet.UpdatePetRequest;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.RescueOrganizationRepository;
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
    private final RescueOrganizationRepository rescueOrganizationRepository;

    public PetController(PetService petService, RescueOrganizationRepository rescueOrganizationRepository) {
        this.petService = petService;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
    }

    /**
     * Helper to get the rescue organization for a user.
     */
    private RescueOrganization getRescueOrgForUser(java.util.UUID userId) {
        return rescueOrganizationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue organization profile not found for user"));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PetDto>> getAvailablePets(
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String sex,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        // Limit page size to prevent abuse
        int effectivePageSize = Math.min(pageSize, 50);
        return ResponseEntity.ok(petService.getAvailablePetsPaged(
                species, size, sex, minAge, maxAge, page, effectivePageSize));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<PetDto>> getFeaturedPets() {
        return ResponseEntity.ok(petService.getFeaturedPets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetDto> getPet(@PathVariable UUID id) {
        return ResponseEntity.ok(petService.getPet(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('FOSTER') or hasRole('RESCUE_ORG')")
    public ResponseEntity<PetDto> createPet(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePetRequest request) {
        PetDto pet;
        if (principal.isRescueOrg()) {
            pet = petService.createPetForRescue(principal.userId(), request);
        } else {
            pet = petService.createPet(principal.userId(), request);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(pet);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FOSTER') or hasRole('RESCUE_ORG')")
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
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());
        PetDto pet = petService.acceptByRescue(id, rescueOrg.getId(), principal.userId());
        return ResponseEntity.ok(pet);
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<PetDto> declineByRescue(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeclinePetRequest request) {
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());
        PetDto pet = petService.declineByRescue(id, rescueOrg.getId(), principal.userId(), request.reason());
        return ResponseEntity.ok(pet);
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('FOSTER') or hasRole('RESCUE_ORG')")
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

    /**
     * Get pets created directly by the authenticated rescue organization (no foster involved).
     */
    @GetMapping("/rescue/my/owned")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<List<PetDto>> getMyRescueOwnedPets(@AuthenticationPrincipal UserPrincipal principal) {
        RescueOrganization rescueOrg = getRescueOrgForUser(principal.userId());
        return ResponseEntity.ok(petService.getRescueOwnedPets(rescueOrg.getId()));
    }

    @GetMapping("/rescue/{rescueOrgId}")
    @PreAuthorize("hasRole('RESCUE_ORG') or hasRole('ADMIN')")
    public ResponseEntity<List<PetDto>> getPetsByRescueOrg(
            @PathVariable UUID rescueOrgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify ownership: rescue org users can only access their own organization's pets
        if (principal.isRescueOrg()) {
            RescueOrganization userOrg = getRescueOrgForUser(principal.userId());
            if (!userOrg.getId().equals(rescueOrgId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(petService.getPetsByRescueOrg(rescueOrgId));
    }

    @GetMapping("/rescue/{rescueOrgId}/pending")
    @PreAuthorize("hasRole('RESCUE_ORG') or hasRole('ADMIN')")
    public ResponseEntity<List<PetDto>> getPendingPets(
            @PathVariable UUID rescueOrgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify ownership: rescue org users can only access their own organization's pets
        if (principal.isRescueOrg()) {
            RescueOrganization userOrg = getRescueOrgForUser(principal.userId());
            if (!userOrg.getId().equals(rescueOrgId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(petService.getPendingPetsForRescueOrg(rescueOrgId));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasRole('VET')")
    public ResponseEntity<PetDto> lookupByMicrochip(
            @RequestParam String microchip,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(petService.findByMicrochipForVet(microchip, principal.userId()));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<PetStatusHistoryDto>> getStatusHistory(@PathVariable UUID id) {
        List<PetStatusHistory> history = petService.getStatusHistory(id);
        List<PetStatusHistoryDto> dtos = history.stream()
                .map(PetStatusHistoryDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
