package com.example.foreverhome.service;

import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetImage;
import com.example.foreverhome.domain.profile.Foster;
import com.example.foreverhome.dto.pet.PetImageDto;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.FosterRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PetImageService {

    private static final int MAX_IMAGES_PER_PET = 5;
    private static final String PET_IMAGES_FOLDER = "pets";

    private final PetImageRepository petImageRepository;
    private final PetRepository petRepository;
    private final FosterRepository fosterRepository;
    private final S3StorageService storageService;

    public PetImageService(PetImageRepository petImageRepository,
                           PetRepository petRepository,
                           FosterRepository fosterRepository,
                           S3StorageService storageService) {
        this.petImageRepository = petImageRepository;
        this.petRepository = petRepository;
        this.fosterRepository = fosterRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<PetImageDto> getImagesForPet(UUID petId) {
        return petImageRepository.findByPetIdOrderByDisplayOrder(petId).stream()
                .map(PetImageDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getImageUrlsForPet(UUID petId) {
        return petImageRepository.findByPetIdOrderByDisplayOrder(petId).stream()
                .map(PetImage::getUrl)
                .toList();
    }

    public PetImageDto uploadImage(UUID petId, UUID userId, MultipartFile file) {
        Pet pet = findPetOrThrow(petId);
        verifyOwnership(pet, userId);

        int currentCount = petImageRepository.countByPetId(petId);
        if (currentCount >= MAX_IMAGES_PER_PET) {
            throw new ImageLimitExceededException(
                    "Maximum number of images (" + MAX_IMAGES_PER_PET + ") reached for this pet"
            );
        }

        String folder = PET_IMAGES_FOLDER + "/" + petId;
        String imageUrl = storageService.uploadFile(file, folder);

        boolean isPrimary = currentCount == 0;
        int displayOrder = currentCount;

        PetImage image = PetImage.create(petId, imageUrl, isPrimary, displayOrder);
        PetImage savedImage = petImageRepository.save(image);

        return PetImageDto.from(savedImage);
    }

    public void deleteImage(UUID imageId, UUID userId) {
        PetImage image = findImageOrThrow(imageId);
        Pet pet = findPetOrThrow(image.getPetId());
        verifyOwnership(pet, userId);

        // Delete from S3
        storageService.deleteFile(image.getUrl());

        boolean wasPrimary = image.isPrimary();
        UUID petId = image.getPetId();

        // Delete from database
        petImageRepository.delete(image);

        // If deleted image was primary, set the first remaining image as primary
        if (wasPrimary) {
            List<PetImage> remainingImages = petImageRepository.findByPetIdOrderByDisplayOrder(petId);
            if (!remainingImages.isEmpty()) {
                PetImage newPrimary = remainingImages.getFirst();
                newPrimary.setPrimary(true);
                petImageRepository.save(newPrimary);
            }
        }

        // Reorder remaining images
        reorderImages(petId);
    }

    public PetImageDto setPrimaryImage(UUID imageId, UUID userId) {
        PetImage image = findImageOrThrow(imageId);
        Pet pet = findPetOrThrow(image.getPetId());
        verifyOwnership(pet, userId);

        // Clear current primary
        petImageRepository.clearPrimaryByPetId(image.getPetId());

        // Set new primary
        image.setPrimary(true);
        PetImage savedImage = petImageRepository.save(image);

        return PetImageDto.from(savedImage);
    }

    public List<PetImageDto> reorderImages(UUID petId, UUID userId, List<UUID> imageIds) {
        Pet pet = findPetOrThrow(petId);
        verifyOwnership(pet, userId);

        List<PetImage> images = petImageRepository.findByPetIdOrderByDisplayOrder(petId);

        if (images.size() != imageIds.size()) {
            throw new IllegalArgumentException("Image count mismatch");
        }

        for (int i = 0; i < imageIds.size(); i++) {
            UUID imageId = imageIds.get(i);
            PetImage image = images.stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("PetImage", imageId));
            image.setDisplayOrder(i);
            petImageRepository.save(image);
        }

        return petImageRepository.findByPetIdOrderByDisplayOrder(petId).stream()
                .map(PetImageDto::from)
                .toList();
    }

    private void reorderImages(UUID petId) {
        List<PetImage> images = petImageRepository.findByPetIdOrderByDisplayOrder(petId);
        for (int i = 0; i < images.size(); i++) {
            PetImage image = images.get(i);
            if (image.getDisplayOrder() != i) {
                image.setDisplayOrder(i);
                petImageRepository.save(image);
            }
        }
    }

    private Pet findPetOrThrow(UUID petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", petId));
    }

    private PetImage findImageOrThrow(UUID imageId) {
        return petImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("PetImage", imageId));
    }

    private void verifyOwnership(Pet pet, UUID userId) {
        // Look up the foster profile by user ID to get the foster's ID
        Foster foster = fosterRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("You do not have permission to modify images for this pet"));

        if (!pet.getFosterId().equals(foster.getId())) {
            throw new AccessDeniedException("You do not have permission to modify images for this pet");
        }
    }

    public static class ImageLimitExceededException extends RuntimeException {
        public ImageLimitExceededException(String message) {
            super(message);
        }
    }
}
