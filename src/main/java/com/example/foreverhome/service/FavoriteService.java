package com.example.foreverhome.service;

import com.example.foreverhome.domain.adoption.Favorite;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetImage;
import com.example.foreverhome.domain.profile.Adopter;
import com.example.foreverhome.dto.pet.PetDto;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.AdopterRepository;
import com.example.foreverhome.repository.FavoriteRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PetRepository petRepository;
    private final PetImageRepository petImageRepository;
    private final AdopterRepository adopterRepository;
    private final NotificationService notificationService;
    private final S3StorageService storageService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           PetRepository petRepository,
                           PetImageRepository petImageRepository,
                           AdopterRepository adopterRepository,
                           NotificationService notificationService,
                           S3StorageService storageService) {
        this.favoriteRepository = favoriteRepository;
        this.petRepository = petRepository;
        this.petImageRepository = petImageRepository;
        this.adopterRepository = adopterRepository;
        this.notificationService = notificationService;
        this.storageService = storageService;
    }

    private PetDto toPetDtoWithImages(Pet pet) {
        List<String> imageUrls = petImageRepository.findByPetIdOrderByDisplayOrder(pet.getId())
                .stream()
                .map(img -> storageService.getPublicUrl(img.getS3Key()))
                .toList();
        return PetDto.from(pet, imageUrls);
    }

    /**
     * Look up the adopter profile by user ID.
     */
    private Adopter getAdopterByUserId(UUID userId) {
        return adopterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Adopter profile not found for user"));
    }

    public Favorite addFavorite(UUID userId, UUID petId) {
        // Look up the adopter profile by user ID
        Adopter adopter = getAdopterByUserId(userId);
        UUID adopterId = adopter.getId();

        // Verify pet exists
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", petId));

        // Check if already favorited
        if (favoriteRepository.existsByAdopterIdAndPetId(adopterId, petId)) {
            return favoriteRepository.findByAdopterIdAndPetId(adopterId, petId)
                    .orElseThrow(() -> new ResourceNotFoundException("Favorite", "adopterId and petId"));
        }

        Favorite favorite = Favorite.create(adopterId, petId);
        return favoriteRepository.save(favorite);
    }

    public void removeFavorite(UUID userId, UUID petId) {
        Adopter adopter = getAdopterByUserId(userId);
        favoriteRepository.deleteByAdopterIdAndPetId(adopter.getId(), petId);
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesForAdopter(UUID userId) {
        Adopter adopter = getAdopterByUserId(userId);
        return favoriteRepository.findByAdopterId(adopter.getId());
    }

    @Transactional(readOnly = true)
    public List<PetDto> getFavoritePetsForAdopter(UUID userId) {
        Adopter adopter = getAdopterByUserId(userId);
        List<Favorite> favorites = favoriteRepository.findByAdopterId(adopter.getId());

        if (favorites.isEmpty()) {
            return List.of();
        }

        // Batch fetch all pets in one query to avoid N+1
        List<UUID> petIds = favorites.stream()
                .map(Favorite::getPetId)
                .toList();
        List<Pet> pets = petRepository.findByIdIn(petIds);

        return pets.stream()
                .map(this::toPetDtoWithImages)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(UUID userId, UUID petId) {
        Adopter adopter = getAdopterByUserId(userId);
        return favoriteRepository.existsByAdopterIdAndPetId(adopter.getId(), petId);
    }

    public void notifyFavoritorsOfAvailability(UUID petId, String petName) {
        List<Favorite> favorites = favoriteRepository.findByPetId(petId);
        for (Favorite favorite : favorites) {
            notificationService.notifyFavoriteAvailable(favorite.getAdopterId(), petName);
        }
    }
}
