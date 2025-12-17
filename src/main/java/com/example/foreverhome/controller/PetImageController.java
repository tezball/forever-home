package com.example.foreverhome.controller;

import com.example.foreverhome.dto.pet.PetImageDto;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.PetImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pets/{petId}/images")
public class PetImageController {

    private final PetImageService petImageService;

    public PetImageController(PetImageService petImageService) {
        this.petImageService = petImageService;
    }

    @GetMapping
    public ResponseEntity<List<PetImageDto>> getImages(@PathVariable UUID petId) {
        return ResponseEntity.ok(petImageService.getImagesForPet(petId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FOSTER') or hasRole('RESCUE_ORG')")
    public ResponseEntity<PetImageDto> uploadImage(
            @PathVariable UUID petId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        PetImageDto image = petImageService.uploadImage(petId, principal.userId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(image);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('FOSTER') or hasRole('RESCUE_ORG')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID petId,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        petImageService.deleteImage(imageId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{imageId}/primary")
    @PreAuthorize("hasRole('FOSTER') or hasRole('RESCUE_ORG')")
    public ResponseEntity<PetImageDto> setPrimary(
            @PathVariable UUID petId,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        PetImageDto image = petImageService.setPrimaryImage(imageId, principal.userId());
        return ResponseEntity.ok(image);
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasRole('FOSTER') or hasRole('RESCUE_ORG')")
    public ResponseEntity<List<PetImageDto>> reorderImages(
            @PathVariable UUID petId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody List<UUID> imageIds) {
        List<PetImageDto> images = petImageService.reorderImages(petId, principal.userId(), imageIds);
        return ResponseEntity.ok(images);
    }
}
