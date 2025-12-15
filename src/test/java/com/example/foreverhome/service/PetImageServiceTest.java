package com.example.foreverhome.service;

import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetImage;
import com.example.foreverhome.domain.pet.Species;
import com.example.foreverhome.domain.pet.AgeUnit;
import com.example.foreverhome.domain.pet.PetSex;
import com.example.foreverhome.domain.pet.PetSize;
import com.example.foreverhome.domain.profile.Foster;
import com.example.foreverhome.dto.pet.PetImageDto;
import com.example.foreverhome.exception.AccessDeniedException;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.FosterRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.service.PetImageService.ImageLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PetImageService")
class PetImageServiceTest {

    @Mock
    private PetImageRepository petImageRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private FosterRepository fosterRepository;

    @Mock
    private S3StorageService storageService;

    private PetImageService petImageService;

    @BeforeEach
    void setUp() {
        petImageService = new PetImageService(petImageRepository, petRepository, fosterRepository, storageService);
    }

    @Nested
    @DisplayName("getImagesForPet")
    class GetImagesForPet {

        @Test
        @DisplayName("given pet with images, when getImagesForPet, then returns list of image DTOs")
        void givenPetWithImages_whenGetImagesForPet_thenReturnsListOfImageDtos() {
            // Given
            UUID petId = UUID.randomUUID();
            PetImage image1 = createPetImage(petId, "pets/MICRO123/1.jpg", true, 0);
            PetImage image2 = createPetImage(petId, "pets/MICRO123/2.jpg", false, 1);

            when(petImageRepository.findByPetIdOrderByDisplayOrder(petId)).thenReturn(List.of(image1, image2));
            when(storageService.getPublicUrl(anyString())).thenAnswer(inv -> "https://s3.example.com/" + inv.getArgument(0));

            // When
            List<PetImageDto> result = petImageService.getImagesForPet(petId);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("given pet with no images, when getImagesForPet, then returns empty list")
        void givenPetWithNoImages_whenGetImagesForPet_thenReturnsEmptyList() {
            // Given
            UUID petId = UUID.randomUUID();
            when(petImageRepository.findByPetIdOrderByDisplayOrder(petId)).thenReturn(List.of());

            // When
            List<PetImageDto> result = petImageService.getImagesForPet(petId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getImageUrlsForPet")
    class GetImageUrlsForPet {

        @Test
        @DisplayName("given pet with images, when getImageUrlsForPet, then returns list of URLs")
        void givenPetWithImages_whenGetImageUrlsForPet_thenReturnsListOfUrls() {
            // Given
            UUID petId = UUID.randomUUID();
            PetImage image = createPetImage(petId, "pets/MICRO123/1.jpg", true, 0);

            when(petImageRepository.findByPetIdOrderByDisplayOrder(petId)).thenReturn(List.of(image));
            when(storageService.getPublicUrl("pets/MICRO123/1.jpg")).thenReturn("https://s3.example.com/pets/MICRO123/1.jpg");

            // When
            List<String> result = petImageService.getImageUrlsForPet(petId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo("https://s3.example.com/pets/MICRO123/1.jpg");
        }
    }

    @Nested
    @DisplayName("uploadImage")
    class UploadImage {

        @Test
        @DisplayName("given valid pet and owner, when uploadImage, then uploads and saves image")
        void givenValidPetAndOwner_whenUploadImage_thenUploadsAndSavesImage() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            Pet pet = createPet(petId, fosterId);
            Foster foster = createFoster(userId, fosterId);
            MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "image data".getBytes());

            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));
            when(petImageRepository.countByPetId(petId)).thenReturn(0);
            when(petImageRepository.save(any(PetImage.class))).thenAnswer(inv -> {
                PetImage img = inv.getArgument(0);
                setId(img, UUID.randomUUID());
                return img;
            });
            when(storageService.getPublicUrl(anyString())).thenReturn("https://s3.example.com/image.jpg");

            // When
            PetImageDto result = petImageService.uploadImage(petId, userId, file);

            // Then
            assertThat(result).isNotNull();
            verify(storageService).uploadFileWithKey(eq(file), contains("pets/MICRO123/1.jpg"));

            ArgumentCaptor<PetImage> captor = ArgumentCaptor.forClass(PetImage.class);
            verify(petImageRepository).save(captor.capture());
            assertThat(captor.getValue().isPrimary()).isTrue(); // First image should be primary
            assertThat(captor.getValue().getDisplayOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("given pet already has max images, when uploadImage, then throws ImageLimitExceededException")
        void givenPetAlreadyHasMaxImages_whenUploadImage_thenThrowsImageLimitExceededException() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            Pet pet = createPet(petId, fosterId);
            Foster foster = createFoster(userId, fosterId);
            MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "data".getBytes());

            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));
            when(petImageRepository.countByPetId(petId)).thenReturn(5); // Max is 5

            // When/Then
            assertThatThrownBy(() -> petImageService.uploadImage(petId, userId, file))
                    .isInstanceOf(ImageLimitExceededException.class)
                    .hasMessageContaining("Maximum number of images");
        }

        @Test
        @DisplayName("given non-owner user, when uploadImage, then throws AccessDeniedException")
        void givenNonOwnerUser_whenUploadImage_thenThrowsAccessDeniedException() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID petFosterId = UUID.randomUUID();
            UUID differentFosterId = UUID.randomUUID();
            Pet pet = createPet(petId, petFosterId);
            Foster foster = createFoster(userId, differentFosterId);
            MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "data".getBytes());

            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));

            // When/Then
            assertThatThrownBy(() -> petImageService.uploadImage(petId, userId, file))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("do not have permission");
        }

        @Test
        @DisplayName("given pet not found, when uploadImage, then throws ResourceNotFoundException")
        void givenPetNotFound_whenUploadImage_thenThrowsResourceNotFoundException() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "data".getBytes());

            when(petRepository.findById(petId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> petImageService.uploadImage(petId, userId, file))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Pet");
        }

        @Test
        @DisplayName("given second image upload, when uploadImage, then sets not primary")
        void givenSecondImageUpload_whenUploadImage_thenSetsNotPrimary() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            Pet pet = createPet(petId, fosterId);
            Foster foster = createFoster(userId, fosterId);
            MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "data".getBytes());

            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));
            when(petImageRepository.countByPetId(petId)).thenReturn(1); // Already has one image
            when(petImageRepository.save(any(PetImage.class))).thenAnswer(inv -> {
                PetImage img = inv.getArgument(0);
                setId(img, UUID.randomUUID());
                return img;
            });
            when(storageService.getPublicUrl(anyString())).thenReturn("https://s3.example.com/image.jpg");

            // When
            petImageService.uploadImage(petId, userId, file);

            // Then
            ArgumentCaptor<PetImage> captor = ArgumentCaptor.forClass(PetImage.class);
            verify(petImageRepository).save(captor.capture());
            assertThat(captor.getValue().isPrimary()).isFalse();
            assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("deleteImage")
    class DeleteImage {

        @Test
        @DisplayName("given valid image and owner, when deleteImage, then deletes from S3 and database")
        void givenValidImageAndOwner_whenDeleteImage_thenDeletesFromS3AndDatabase() {
            // Given
            UUID imageId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            PetImage image = createPetImage(petId, "pets/MICRO123/1.jpg", false, 0);
            setId(image, imageId);
            Pet pet = createPet(petId, fosterId);
            Foster foster = createFoster(userId, fosterId);

            when(petImageRepository.findById(imageId)).thenReturn(Optional.of(image));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));
            when(petImageRepository.findByPetIdOrderByDisplayOrder(petId)).thenReturn(List.of());

            // When
            petImageService.deleteImage(imageId, userId);

            // Then
            verify(storageService).deleteFile("pets/MICRO123/1.jpg");
            verify(petImageRepository).delete(image);
        }

        @Test
        @DisplayName("given primary image deleted, when deleteImage, then promotes next image to primary")
        void givenPrimaryImageDeleted_whenDeleteImage_thenPromotesNextImageToPrimary() {
            // Given
            UUID imageId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            PetImage primaryImage = createPetImage(petId, "pets/MICRO123/1.jpg", true, 0);
            setId(primaryImage, imageId);
            PetImage secondImage = createPetImage(petId, "pets/MICRO123/2.jpg", false, 1);
            setId(secondImage, UUID.randomUUID());
            Pet pet = createPet(petId, fosterId);
            Foster foster = createFoster(userId, fosterId);

            when(petImageRepository.findById(imageId)).thenReturn(Optional.of(primaryImage));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));
            when(petImageRepository.findByPetIdOrderByDisplayOrder(petId)).thenReturn(List.of(secondImage));

            // When
            petImageService.deleteImage(imageId, userId);

            // Then - verify the next image was promoted to primary (save may be called multiple times due to reordering)
            verify(petImageRepository, atLeastOnce()).save(argThat(img -> img.isPrimary()));
        }

        @Test
        @DisplayName("given image not found, when deleteImage, then throws ResourceNotFoundException")
        void givenImageNotFound_whenDeleteImage_thenThrowsResourceNotFoundException() {
            // Given
            UUID imageId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(petImageRepository.findById(imageId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> petImageService.deleteImage(imageId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("PetImage");
        }
    }

    @Nested
    @DisplayName("setPrimaryImage")
    class SetPrimaryImage {

        @Test
        @DisplayName("given valid image, when setPrimaryImage, then clears old primary and sets new")
        void givenValidImage_whenSetPrimaryImage_thenClearsOldPrimaryAndSetsNew() {
            // Given
            UUID imageId = UUID.randomUUID();
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            PetImage image = createPetImage(petId, "pets/MICRO123/2.jpg", false, 1);
            setId(image, imageId);
            Pet pet = createPet(petId, fosterId);
            Foster foster = createFoster(userId, fosterId);

            when(petImageRepository.findById(imageId)).thenReturn(Optional.of(image));
            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));
            when(petImageRepository.save(any(PetImage.class))).thenAnswer(inv -> inv.getArgument(0));
            when(storageService.getPublicUrl(anyString())).thenReturn("https://s3.example.com/image.jpg");

            // When
            PetImageDto result = petImageService.setPrimaryImage(imageId, userId);

            // Then
            verify(petImageRepository).clearPrimaryByPetId(petId);
            ArgumentCaptor<PetImage> captor = ArgumentCaptor.forClass(PetImage.class);
            verify(petImageRepository).save(captor.capture());
            assertThat(captor.getValue().isPrimary()).isTrue();
        }
    }

    @Nested
    @DisplayName("reorderImages")
    class ReorderImages {

        @Test
        @DisplayName("given valid image IDs, when reorderImages, then updates display order")
        void givenValidImageIds_whenReorderImages_thenUpdatesDisplayOrder() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            UUID imageId1 = UUID.randomUUID();
            UUID imageId2 = UUID.randomUUID();
            Pet pet = createPet(petId, fosterId);
            Foster foster = createFoster(userId, fosterId);
            PetImage image1 = createPetImage(petId, "key1", true, 0);
            PetImage image2 = createPetImage(petId, "key2", false, 1);
            setId(image1, imageId1);
            setId(image2, imageId2);

            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));
            when(petImageRepository.findByPetIdOrderByDisplayOrder(petId)).thenReturn(List.of(image1, image2));
            when(petImageRepository.save(any(PetImage.class))).thenAnswer(inv -> inv.getArgument(0));
            when(storageService.getPublicUrl(anyString())).thenReturn("https://s3.example.com/image.jpg");

            // When - reorder to put image2 first
            List<PetImageDto> result = petImageService.reorderImages(petId, userId, List.of(imageId2, imageId1));

            // Then
            verify(petImageRepository, times(2)).save(any(PetImage.class));
        }

        @Test
        @DisplayName("given mismatched image count, when reorderImages, then throws IllegalArgumentException")
        void givenMismatchedImageCount_whenReorderImages_thenThrowsIllegalArgumentException() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID fosterId = UUID.randomUUID();
            Pet pet = createPet(petId, fosterId);
            Foster foster = createFoster(userId, fosterId);
            PetImage image = createPetImage(petId, "key1", true, 0);
            setId(image, UUID.randomUUID());

            when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
            when(fosterRepository.findByUserId(userId)).thenReturn(Optional.of(foster));
            when(petImageRepository.findByPetIdOrderByDisplayOrder(petId)).thenReturn(List.of(image));

            // When/Then - providing 2 IDs when only 1 image exists
            assertThatThrownBy(() -> petImageService.reorderImages(petId, userId, List.of(UUID.randomUUID(), UUID.randomUUID())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("count mismatch");
        }
    }

    // Helper methods
    private Pet createPet(UUID petId, UUID fosterId) {
        Pet pet = Pet.create(fosterId, "Buddy", Species.DOG, "Labrador", 2, AgeUnit.YEARS, PetSex.MALE, PetSize.LARGE,
                "MICRO123", "A friendly dog", "Healthy");
        setId(pet, petId);
        return pet;
    }

    private Foster createFoster(UUID userId, UUID fosterId) {
        Foster foster = Foster.create(userId, "John", "Doe", "555-1234", null);
        setId(foster, fosterId);
        return foster;
    }

    private PetImage createPetImage(UUID petId, String s3Key, boolean isPrimary, int displayOrder) {
        return PetImage.create(petId, s3Key, isPrimary, displayOrder);
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
