package com.example.foreverhome.service;

import com.example.foreverhome.domain.adoption.Favorite;
import com.example.foreverhome.domain.pet.Breed;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetImage;
import com.example.foreverhome.domain.pet.Species;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.profile.Adopter;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.AdopterRepository;
import com.example.foreverhome.repository.FavoriteRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetImageRepository petImageRepository;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private S3StorageService storageService;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(
                favoriteRepository,
                petRepository,
                petImageRepository,
                adopterRepository,
                notificationService,
                storageService
        );
    }

    @Nested
    @DisplayName("addFavorite")
    class AddFavorite {

        @Test
        @DisplayName("given valid user and pet, when addFavorite, then creates and saves favorite")
        void givenValidUserAndPet_whenAddFavorite_thenCreatesAndSavesFavorite() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);
            Pet pet = createPet(petId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(favoriteRepository.existsByAdopterIdAndPetId(adopter.getId(), petId)).thenReturn(false);
            when(favoriteRepository.save(any(Favorite.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Favorite result = favoriteService.addFavorite(userId, petId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAdopterId()).isEqualTo(adopter.getId());
            assertThat(result.getPetId()).isEqualTo(petId);

            ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
            verify(favoriteRepository).save(captor.capture());
            assertThat(captor.getValue().getPetId()).isEqualTo(petId);
        }

        @Test
        @DisplayName("given pet already favorited, when addFavorite, then returns existing favorite")
        void givenPetAlreadyFavorited_whenAddFavorite_thenReturnsExistingFavorite() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);
            Pet pet = createPet(petId);
            Favorite existingFavorite = Favorite.create(adopter.getId(), petId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(favoriteRepository.existsByAdopterIdAndPetId(adopter.getId(), petId)).thenReturn(true);
            when(favoriteRepository.findByAdopterIdAndPetId(adopter.getId(), petId)).thenReturn(Optional.of(existingFavorite));

            // When
            Favorite result = favoriteService.addFavorite(userId, petId);

            // Then
            assertThat(result).isEqualTo(existingFavorite);
            verify(favoriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("given adopter profile not found, when addFavorite, then throws ResourceNotFoundException")
        void givenAdopterProfileNotFound_whenAddFavorite_thenThrowsResourceNotFoundException() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> favoriteService.addFavorite(userId, petId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Adopter profile not found");
        }

        @Test
        @DisplayName("given pet not found, when addFavorite, then throws ResourceNotFoundException")
        void givenPetNotFound_whenAddFavorite_thenThrowsResourceNotFoundException() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(petRepository.findById(petId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> favoriteService.addFavorite(userId, petId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Pet");
        }
    }

    @Nested
    @DisplayName("removeFavorite")
    class RemoveFavorite {

        @Test
        @DisplayName("given valid user and favorited pet, when removeFavorite, then deletes favorite")
        void givenValidUserAndFavoritedPet_whenRemoveFavorite_thenDeletesFavorite() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));

            // When
            favoriteService.removeFavorite(userId, petId);

            // Then
            verify(favoriteRepository).deleteByAdopterIdAndPetId(adopter.getId(), petId);
        }

        @Test
        @DisplayName("given adopter profile not found, when removeFavorite, then throws ResourceNotFoundException")
        void givenAdopterProfileNotFound_whenRemoveFavorite_thenThrowsResourceNotFoundException() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> favoriteService.removeFavorite(userId, petId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Adopter profile not found");
        }
    }

    @Nested
    @DisplayName("getFavoritesForAdopter")
    class GetFavoritesForAdopter {

        @Test
        @DisplayName("given adopter with favorites, when getFavoritesForAdopter, then returns list of favorites")
        void givenAdopterWithFavorites_whenGetFavoritesForAdopter_thenReturnsListOfFavorites() {
            // Given
            UUID userId = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);
            Favorite favorite1 = Favorite.create(adopter.getId(), UUID.randomUUID());
            Favorite favorite2 = Favorite.create(adopter.getId(), UUID.randomUUID());

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(favoriteRepository.findByAdopterId(adopter.getId())).thenReturn(List.of(favorite1, favorite2));

            // When
            List<Favorite> result = favoriteService.getFavoritesForAdopter(userId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(favorite1, favorite2);
        }

        @Test
        @DisplayName("given adopter with no favorites, when getFavoritesForAdopter, then returns empty list")
        void givenAdopterWithNoFavorites_whenGetFavoritesForAdopter_thenReturnsEmptyList() {
            // Given
            UUID userId = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(favoriteRepository.findByAdopterId(adopter.getId())).thenReturn(List.of());

            // When
            List<Favorite> result = favoriteService.getFavoritesForAdopter(userId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFavoritePetsForAdopter")
    class GetFavoritePetsForAdopter {

        @Test
        @DisplayName("given adopter with favorited pets, when getFavoritePetsForAdopter, then returns pet DTOs with images")
        void givenAdopterWithFavoritedPets_whenGetFavoritePetsForAdopter_thenReturnsPetDtosWithImages() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId1 = UUID.randomUUID();
            UUID petId2 = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);
            Favorite favorite1 = Favorite.create(adopter.getId(), petId1);
            Favorite favorite2 = Favorite.create(adopter.getId(), petId2);
            Pet pet1 = createPet(petId1);
            Pet pet2 = createPet(petId2);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(favoriteRepository.findByAdopterId(adopter.getId())).thenReturn(List.of(favorite1, favorite2));
            when(petRepository.findByIdIn(List.of(petId1, petId2))).thenReturn(List.of(pet1, pet2));
            when(petImageRepository.findByPetIdOrderByDisplayOrder(any())).thenReturn(List.of());

            // When
            List<PetDto> result = favoriteService.getFavoritePetsForAdopter(userId);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("given adopter with no favorites, when getFavoritePetsForAdopter, then returns empty list")
        void givenAdopterWithNoFavorites_whenGetFavoritePetsForAdopter_thenReturnsEmptyList() {
            // Given
            UUID userId = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(favoriteRepository.findByAdopterId(adopter.getId())).thenReturn(List.of());

            // When
            List<PetDto> result = favoriteService.getFavoritePetsForAdopter(userId);

            // Then
            assertThat(result).isEmpty();
            verify(petRepository, never()).findByIdIn(any());
        }
    }

    @Nested
    @DisplayName("isFavorited")
    class IsFavorited {

        @Test
        @DisplayName("given pet is favorited by adopter, when isFavorited, then returns true")
        void givenPetIsFavoritedByAdopter_whenIsFavorited_thenReturnsTrue() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(favoriteRepository.existsByAdopterIdAndPetId(adopter.getId(), petId)).thenReturn(true);

            // When
            boolean result = favoriteService.isFavorited(userId, petId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("given pet is not favorited by adopter, when isFavorited, then returns false")
        void givenPetIsNotFavoritedByAdopter_whenIsFavorited_thenReturnsFalse() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            Adopter adopter = createAdopter(userId);

            when(adopterRepository.findByUserId(userId)).thenReturn(Optional.of(adopter));
            when(favoriteRepository.existsByAdopterIdAndPetId(adopter.getId(), petId)).thenReturn(false);

            // When
            boolean result = favoriteService.isFavorited(userId, petId);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("notifyFavoritorsOfAvailability")
    class NotifyFavoritorsOfAvailability {

        @Test
        @DisplayName("given pet with favoritors, when notifyFavoritorsOfAvailability, then notifies all favoritors")
        void givenPetWithFavoritors_whenNotifyFavoritorsOfAvailability_thenNotifiesAllFavoritors() {
            // Given
            UUID petId = UUID.randomUUID();
            String petName = "Buddy";
            UUID adopterId1 = UUID.randomUUID();
            UUID adopterId2 = UUID.randomUUID();
            Favorite favorite1 = Favorite.create(adopterId1, petId);
            Favorite favorite2 = Favorite.create(adopterId2, petId);

            when(favoriteRepository.findByPetId(petId)).thenReturn(List.of(favorite1, favorite2));

            // When
            favoriteService.notifyFavoritorsOfAvailability(petId, petName);

            // Then
            verify(notificationService).notifyFavoriteAvailable(adopterId1, petName);
            verify(notificationService).notifyFavoriteAvailable(adopterId2, petName);
        }

        @Test
        @DisplayName("given pet with no favoritors, when notifyFavoritorsOfAvailability, then no notifications sent")
        void givenPetWithNoFavoritors_whenNotifyFavoritorsOfAvailability_thenNoNotificationsSent() {
            // Given
            UUID petId = UUID.randomUUID();
            String petName = "Buddy";

            when(favoriteRepository.findByPetId(petId)).thenReturn(List.of());

            // When
            favoriteService.notifyFavoritorsOfAvailability(petId, petName);

            // Then
            verify(notificationService, never()).notifyFavoriteAvailable(any(), any());
        }
    }

    @Nested
    @DisplayName("notifyFavoritorsOfAdoption")
    class NotifyFavoritorsOfAdoption {

        @Test
        @DisplayName("given pet with favoritors, when notifyFavoritorsOfAdoption, then notifies all favoritors")
        void givenPetWithFavoritors_whenNotifyFavoritorsOfAdoption_thenNotifiesAllFavoritors() {
            // Given
            UUID petId = UUID.randomUUID();
            String petName = "Buddy";
            UUID adopterId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Favorite favorite = Favorite.create(adopterId, petId);
            Adopter adopter = createAdopterWithId(adopterId, userId);

            when(favoriteRepository.findByPetId(petId)).thenReturn(List.of(favorite));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.of(adopter));

            // When
            favoriteService.notifyFavoritorsOfAdoption(petId, petName);

            // Then
            verify(notificationService).notifyFavoritePetAdopted(userId, petName, petId);
        }

        @Test
        @DisplayName("given adopter not found, when notifyFavoritorsOfAdoption, then skips notification for that adopter")
        void givenAdopterNotFound_whenNotifyFavoritorsOfAdoption_thenSkipsNotification() {
            // Given
            UUID petId = UUID.randomUUID();
            String petName = "Buddy";
            UUID adopterId = UUID.randomUUID();
            Favorite favorite = Favorite.create(adopterId, petId);

            when(favoriteRepository.findByPetId(petId)).thenReturn(List.of(favorite));
            when(adopterRepository.findById(adopterId)).thenReturn(Optional.empty());

            // When
            favoriteService.notifyFavoritorsOfAdoption(petId, petName);

            // Then
            verify(notificationService, never()).notifyFavoritePetAdopted(any(), any(), any());
        }
    }

    // Helper methods
    private Adopter createAdopter(UUID userId) {
        Adopter adopter = Adopter.create(userId, "John", "Doe", "555-1234", "House", "5 years", null);
        setId(adopter, UUID.randomUUID());
        return adopter;
    }

    private Adopter createAdopterWithId(UUID adopterId, UUID userId) {
        Adopter adopter = Adopter.create(userId, "John", "Doe", "555-1234", "House", "5 years", null);
        setId(adopter, adopterId);
        return adopter;
    }

    private Pet createPet(UUID petId) {
        Pet pet = Pet.create(
                UUID.randomUUID(),
                "Buddy",
                Species.DOG,
                Breed.LABRADOR,
                2,
                AgeUnit.YEARS,
                PetSex.MALE,
                PetSize.LARGE,
                "MICRO123",
                "A friendly dog",
                "Healthy"
        );
        setId(pet, petId);
        return pet;
    }

    private void setId(Object entity, UUID id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }
}
