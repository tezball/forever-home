package com.example.foreverhome.service;

import com.example.foreverhome.domain.adoption.Favorite;
import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.FavoriteRepository;
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
    private final NotificationService notificationService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           PetRepository petRepository,
                           NotificationService notificationService) {
        this.favoriteRepository = favoriteRepository;
        this.petRepository = petRepository;
        this.notificationService = notificationService;
    }

    public Favorite addFavorite(UUID adopterId, UUID petId) {
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

    public void removeFavorite(UUID adopterId, UUID petId) {
        favoriteRepository.deleteByAdopterIdAndPetId(adopterId, petId);
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesForAdopter(UUID adopterId) {
        return favoriteRepository.findByAdopterId(adopterId);
    }

    @Transactional(readOnly = true)
    public List<Pet> getFavoritePetsForAdopter(UUID adopterId) {
        List<Favorite> favorites = favoriteRepository.findByAdopterId(adopterId);
        return favorites.stream()
                .map(fav -> petRepository.findById(fav.getPetId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(UUID adopterId, UUID petId) {
        return favoriteRepository.existsByAdopterIdAndPetId(adopterId, petId);
    }

    public void notifyFavoritorsOfAvailability(UUID petId, String petName) {
        List<Favorite> favorites = favoriteRepository.findByPetId(petId);
        for (Favorite favorite : favorites) {
            notificationService.notifyFavoriteAvailable(favorite.getAdopterId(), petName);
        }
    }
}
