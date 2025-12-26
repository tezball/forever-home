package com.example.foreverhome.controller;

import com.example.foreverhome.domain.adoption.Favorite;
import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.Breed;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.pet.Species;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.FavoriteService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteController")
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private FavoriteController favoriteController;

    private UserPrincipal adopterPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adopterPrincipal = new UserPrincipal(userId, UserRole.ADOPTER, true);
    }

    @Nested
    @DisplayName("GET /api/favorites")
    class GetFavorites {

        @Test
        @DisplayName("given authenticated adopter, when getFavorites, then returns list of favorites")
        void givenAuthenticatedAdopter_whenGetFavorites_thenReturnsListOfFavorites() {
            // Given
            UUID adopterId = UUID.randomUUID();
            Favorite favorite1 = Favorite.create(adopterId, UUID.randomUUID());
            Favorite favorite2 = Favorite.create(adopterId, UUID.randomUUID());
            when(favoriteService.getFavoritesForAdopter(userId)).thenReturn(List.of(favorite1, favorite2));

            // When
            ResponseEntity<List<Favorite>> response = favoriteController.getFavorites(adopterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("given adopter with no favorites, when getFavorites, then returns empty list")
        void givenAdopterWithNoFavorites_whenGetFavorites_thenReturnsEmptyList() {
            // Given
            when(favoriteService.getFavoritesForAdopter(userId)).thenReturn(List.of());

            // When
            ResponseEntity<List<Favorite>> response = favoriteController.getFavorites(adopterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/favorites/pets")
    class GetFavoritePets {

        @Test
        @DisplayName("given adopter with favorites, when getFavoritePets, then returns list of pet DTOs")
        void givenAdopterWithFavorites_whenGetFavoritePets_thenReturnsListOfPetDtos() {
            // Given
            PetDto pet1 = createPetDto("Buddy");
            PetDto pet2 = createPetDto("Max");
            when(favoriteService.getFavoritePetsForAdopter(userId)).thenReturn(List.of(pet1, pet2));

            // When
            ResponseEntity<List<PetDto>> response = favoriteController.getFavoritePets(adopterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).name()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("given adopter with no favorites, when getFavoritePets, then returns empty list")
        void givenAdopterWithNoFavorites_whenGetFavoritePets_thenReturnsEmptyList() {
            // Given
            when(favoriteService.getFavoritePetsForAdopter(userId)).thenReturn(List.of());

            // When
            ResponseEntity<List<PetDto>> response = favoriteController.getFavoritePets(adopterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("POST /api/favorites/{petId}")
    class AddFavorite {

        @Test
        @DisplayName("given valid pet ID, when addFavorite, then returns created favorite")
        void givenValidPetId_whenAddFavorite_thenReturnsCreatedFavorite() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID adopterId = UUID.randomUUID();
            Favorite favorite = Favorite.create(adopterId, petId);
            when(favoriteService.addFavorite(userId, petId)).thenReturn(favorite);

            // When
            ResponseEntity<Favorite> response = favoriteController.addFavorite(petId, adopterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(favorite);
            verify(favoriteService).addFavorite(userId, petId);
        }
    }

    @Nested
    @DisplayName("DELETE /api/favorites/{petId}")
    class RemoveFavorite {

        @Test
        @DisplayName("given favorited pet, when removeFavorite, then returns no content")
        void givenFavoritedPet_whenRemoveFavorite_thenReturnsNoContent() {
            // Given
            UUID petId = UUID.randomUUID();
            doNothing().when(favoriteService).removeFavorite(userId, petId);

            // When
            ResponseEntity<Void> response = favoriteController.removeFavorite(petId, adopterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(favoriteService).removeFavorite(userId, petId);
        }
    }

    @Nested
    @DisplayName("GET /api/favorites/{petId}/status")
    class IsFavorited {

        @Test
        @DisplayName("given pet is favorited, when isFavorited, then returns true")
        void givenPetIsFavorited_whenIsFavorited_thenReturnsTrue() {
            // Given
            UUID petId = UUID.randomUUID();
            when(favoriteService.isFavorited(userId, petId)).thenReturn(true);

            // When
            ResponseEntity<Boolean> response = favoriteController.isFavorited(petId, adopterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isTrue();
        }

        @Test
        @DisplayName("given pet is not favorited, when isFavorited, then returns false")
        void givenPetIsNotFavorited_whenIsFavorited_thenReturnsFalse() {
            // Given
            UUID petId = UUID.randomUUID();
            when(favoriteService.isFavorited(userId, petId)).thenReturn(false);

            // When
            ResponseEntity<Boolean> response = favoriteController.isFavorited(petId, adopterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isFalse();
        }
    }

    // Helper method
    private PetDto createPetDto(String name) {
        return new PetDto(
                UUID.randomUUID(),
                name,
                Species.DOG,
                Breed.LABRADOR,
                2,
                AgeUnit.YEARS,
                PetSex.MALE,
                PetSize.LARGE,
                "MICRO123",
                "A friendly dog",
                "Healthy",
                PetStatus.AVAILABLE,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                List.of(),
                null,
                null
        );
    }
}
