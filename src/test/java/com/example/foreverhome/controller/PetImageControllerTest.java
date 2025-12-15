package com.example.foreverhome.controller;

import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.pet.PetImageDto;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.PetImageService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PetImageController")
class PetImageControllerTest {

    @Mock
    private PetImageService petImageService;

    @InjectMocks
    private PetImageController petImageController;

    private UserPrincipal fosterPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        fosterPrincipal = new UserPrincipal(userId, UserRole.FOSTER, true);
    }

    @Nested
    @DisplayName("GET /api/pets/{petId}/images")
    class GetImages {

        @Test
        @DisplayName("given pet with images, when getImages, then returns list of image DTOs")
        void givenPetWithImages_whenGetImages_thenReturnsListOfImageDtos() {
            // Given
            UUID petId = UUID.randomUUID();
            PetImageDto image1 = createImageDto(true);
            PetImageDto image2 = createImageDto(false);
            when(petImageService.getImagesForPet(petId)).thenReturn(List.of(image1, image2));

            // When
            ResponseEntity<List<PetImageDto>> response = petImageController.getImages(petId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("given pet with no images, when getImages, then returns empty list")
        void givenPetWithNoImages_whenGetImages_thenReturnsEmptyList() {
            // Given
            UUID petId = UUID.randomUUID();
            when(petImageService.getImagesForPet(petId)).thenReturn(List.of());

            // When
            ResponseEntity<List<PetImageDto>> response = petImageController.getImages(petId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("POST /api/pets/{petId}/images")
    class UploadImage {

        @Test
        @DisplayName("given valid file, when uploadImage, then returns created image DTO")
        void givenValidFile_whenUploadImage_thenReturnsCreatedImageDto() {
            // Given
            UUID petId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "data".getBytes());
            PetImageDto imageDto = createImageDto(true);
            when(petImageService.uploadImage(petId, userId, file)).thenReturn(imageDto);

            // When
            ResponseEntity<PetImageDto> response = petImageController.uploadImage(petId, fosterPrincipal, file);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(imageDto);
            verify(petImageService).uploadImage(petId, userId, file);
        }
    }

    @Nested
    @DisplayName("DELETE /api/pets/{petId}/images/{imageId}")
    class DeleteImage {

        @Test
        @DisplayName("given valid image ID, when deleteImage, then returns no content")
        void givenValidImageId_whenDeleteImage_thenReturnsNoContent() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID imageId = UUID.randomUUID();
            doNothing().when(petImageService).deleteImage(imageId, userId);

            // When
            ResponseEntity<Void> response = petImageController.deleteImage(petId, imageId, fosterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(petImageService).deleteImage(imageId, userId);
        }
    }

    @Nested
    @DisplayName("PUT /api/pets/{petId}/images/{imageId}/primary")
    class SetPrimary {

        @Test
        @DisplayName("given valid image ID, when setPrimary, then returns updated image DTO")
        void givenValidImageId_whenSetPrimary_thenReturnsUpdatedImageDto() {
            // Given
            UUID petId = UUID.randomUUID();
            UUID imageId = UUID.randomUUID();
            PetImageDto imageDto = createImageDto(true);
            when(petImageService.setPrimaryImage(imageId, userId)).thenReturn(imageDto);

            // When
            ResponseEntity<PetImageDto> response = petImageController.setPrimary(petId, imageId, fosterPrincipal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isPrimary()).isTrue();
            verify(petImageService).setPrimaryImage(imageId, userId);
        }
    }

    @Nested
    @DisplayName("PUT /api/pets/{petId}/images/reorder")
    class ReorderImages {

        @Test
        @DisplayName("given valid image IDs, when reorderImages, then returns reordered images")
        void givenValidImageIds_whenReorderImages_thenReturnsReorderedImages() {
            // Given
            UUID petId = UUID.randomUUID();
            List<UUID> imageIds = List.of(UUID.randomUUID(), UUID.randomUUID());
            PetImageDto image1 = createImageDto(true);
            PetImageDto image2 = createImageDto(false);
            when(petImageService.reorderImages(petId, userId, imageIds)).thenReturn(List.of(image1, image2));

            // When
            ResponseEntity<List<PetImageDto>> response = petImageController.reorderImages(petId, fosterPrincipal, imageIds);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            verify(petImageService).reorderImages(petId, userId, imageIds);
        }
    }

    // Helper method
    private PetImageDto createImageDto(boolean isPrimary) {
        return new PetImageDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "https://s3.example.com/image.jpg",
                isPrimary,
                isPrimary ? 0 : 1,
                Instant.now()
        );
    }
}
