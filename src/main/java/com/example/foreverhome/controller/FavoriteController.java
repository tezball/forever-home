package com.example.foreverhome.controller;

import com.example.foreverhome.domain.adoption.Favorite;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("hasRole('ADOPTER')")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<List<Favorite>> getFavorites(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(favoriteService.getFavoritesForAdopter(principal.userId()));
    }

    @GetMapping("/pets")
    public ResponseEntity<List<PetDto>> getFavoritePets(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(favoriteService.getFavoritePetsForAdopter(principal.userId()));
    }

    @PostMapping("/{petId}")
    public ResponseEntity<Favorite> addFavorite(
            @PathVariable UUID petId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Favorite favorite = favoriteService.addFavorite(principal.userId(), petId);
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite);
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable UUID petId,
            @AuthenticationPrincipal UserPrincipal principal) {
        favoriteService.removeFavorite(principal.userId(), petId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{petId}/status")
    public ResponseEntity<Boolean> isFavorited(
            @PathVariable UUID petId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(favoriteService.isFavorited(principal.userId(), petId));
    }
}
