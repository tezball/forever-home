package com.example.foreverhome.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3StorageService")
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private MultipartFile mockFile;

    private static final String BUCKET_NAME = "test-bucket";
    private static final String LOCALSTACK_ENDPOINT = "http://localhost:4566";
    private static final long MAX_FILE_SIZE = 5242880L; // 5MB
    private static final String ALLOWED_CONTENT_TYPES = "image/jpeg,image/png,image/gif,image/webp";

    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        // Default to LocalStack mode for most tests
        s3StorageService = new S3StorageService(
                s3Client,
                s3Presigner,
                BUCKET_NAME,
                LOCALSTACK_ENDPOINT,
                MAX_FILE_SIZE,
                ALLOWED_CONTENT_TYPES
        );
    }

    @Nested
    @DisplayName("uploadFile")
    class UploadFile {

        @Test
        @DisplayName("given valid image file, when upload, then returns S3 key with UUID")
        void givenValidImageFile_whenUpload_thenReturnsS3KeyWithUuid() throws IOException {
            // Given
            String folder = "pets/12345";
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getOriginalFilename()).thenReturn("photo.jpg");
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // When
            String result = s3StorageService.uploadFile(mockFile, folder);

            // Then
            assertThat(result).startsWith(folder + "/");
            assertThat(result).endsWith(".jpg");
            assertThat(result).contains("-"); // UUID contains dashes

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
            assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET_NAME);
            assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("given PNG file, when upload, then preserves png extension")
        void givenPngFile_whenUpload_thenPreservesPngExtension() throws IOException {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("image/png");
            when(mockFile.getOriginalFilename()).thenReturn("image.png");
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // When
            String result = s3StorageService.uploadFile(mockFile, "folder");

            // Then
            assertThat(result).endsWith(".png");
        }

        @Test
        @DisplayName("given file exceeds max size, when upload, then throws StorageException")
        void givenFileExceedsMaxSize_whenUpload_thenThrowsStorageException() {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(6 * 1024 * 1024L); // 6MB

            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, "folder"))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasMessageContaining("exceeds maximum allowed size");
        }

        @Test
        @DisplayName("given disallowed content type, when upload, then throws StorageException")
        void givenDisallowedContentType_whenUpload_thenThrowsStorageException() {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("application/pdf");

            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, "folder"))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasMessageContaining("File type not allowed");
        }

        @Test
        @DisplayName("given null content type, when upload, then throws StorageException")
        void givenNullContentType_whenUpload_thenThrowsStorageException() {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, "folder"))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasMessageContaining("File type not allowed");
        }

        @Test
        @DisplayName("given empty file, when upload, then throws StorageException")
        void givenEmptyFile_whenUpload_thenThrowsStorageException() {
            // Given
            when(mockFile.isEmpty()).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, "folder"))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasMessageContaining("File is empty");
        }

        @Test
        @DisplayName("given null file, when upload, then throws StorageException")
        void givenNullFile_whenUpload_thenThrowsStorageException() {
            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFile(null, "folder"))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasMessageContaining("File is empty");
        }

        @Test
        @DisplayName("given S3Exception during upload, when upload, then wraps in StorageException")
        void givenS3ExceptionDuringUpload_whenUpload_thenWrapsInStorageException() throws IOException {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getOriginalFilename()).thenReturn("photo.jpg");
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));

            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                    .errorCode("AccessDenied")
                    .errorMessage("Access Denied")
                    .build();
            S3Exception s3Exception = (S3Exception) S3Exception.builder()
                    .message("Access Denied")
                    .statusCode(403)
                    .awsErrorDetails(errorDetails)
                    .build();
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(s3Exception);

            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, "folder"))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasCauseInstanceOf(S3Exception.class);
        }

        @Test
        @DisplayName("given IOException reading file, when upload, then wraps in StorageException")
        void givenIOExceptionReadingFile_whenUpload_thenWrapsInStorageException() throws IOException {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getOriginalFilename()).thenReturn("photo.jpg");
            when(mockFile.getInputStream()).thenThrow(new IOException("Disk error"));

            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, "folder"))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("given file without extension, when upload, then generates key without extension")
        void givenFileWithoutExtension_whenUpload_thenGeneratesKeyWithoutExtension() throws IOException {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getOriginalFilename()).thenReturn("photo"); // No extension
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // When
            String result = s3StorageService.uploadFile(mockFile, "folder");

            // Then
            assertThat(result).startsWith("folder/");
            assertThat(result).doesNotContain("."); // No extension added
        }
    }

    @Nested
    @DisplayName("uploadFileWithKey")
    class UploadFileWithKey {

        @Test
        @DisplayName("given valid file and key, when upload with key, then uses exact key")
        void givenValidFileAndKey_whenUploadWithKey_thenUsesExactKey() throws IOException {
            // Given
            String exactKey = "pets/CHIP123/profile.jpg";
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // When
            String result = s3StorageService.uploadFileWithKey(mockFile, exactKey);

            // Then
            assertThat(result).isEqualTo(exactKey);

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
            assertThat(requestCaptor.getValue().key()).isEqualTo(exactKey);
        }

        @Test
        @DisplayName("given invalid file, when upload with key, then throws StorageException")
        void givenInvalidFile_whenUploadWithKey_thenThrowsStorageException() {
            // Given
            when(mockFile.isEmpty()).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFileWithKey(mockFile, "some/key"))
                    .isInstanceOf(S3StorageService.StorageException.class);
        }
    }

    @Nested
    @DisplayName("getPublicUrl")
    class GetPublicUrl {

        @Test
        @DisplayName("given LocalStack endpoint configured, when get URL, then returns direct bucket URL")
        void givenLocalStackEndpointConfigured_whenGetUrl_thenReturnsDirectBucketUrl() {
            // Given - service already configured with LocalStack endpoint in setUp()
            String s3Key = "pets/123/image.jpg";

            // When
            String result = s3StorageService.getPublicUrl(s3Key);

            // Then
            assertThat(result).isEqualTo(LOCALSTACK_ENDPOINT + "/" + BUCKET_NAME + "/" + s3Key);
        }

        @Test
        @DisplayName("given AWS environment (no endpoint), when get URL, then returns presigned URL")
        void givenAwsEnvironment_whenGetUrl_thenReturnsPresignedUrl() throws Exception {
            // Given - create service without endpoint (AWS mode)
            S3StorageService awsService = new S3StorageService(
                    s3Client,
                    s3Presigner,
                    BUCKET_NAME,
                    "", // Empty endpoint = AWS mode
                    MAX_FILE_SIZE,
                    ALLOWED_CONTENT_TYPES
            );

            String s3Key = "pets/123/image.jpg";
            String presignedUrl = "https://test-bucket.s3.amazonaws.com/pets/123/image.jpg?X-Amz-Signature=abc123";

            PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
            when(presignedRequest.url()).thenReturn(new URL(presignedUrl));
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

            // When
            String result = awsService.getPublicUrl(s3Key);

            // Then
            assertThat(result).isEqualTo(presignedUrl);
            verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("given null key, when get URL, then returns null")
        void givenNullKey_whenGetUrl_thenReturnsNull() {
            // When
            String result = s3StorageService.getPublicUrl(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("given blank key, when get URL, then returns null")
        void givenBlankKey_whenGetUrl_thenReturnsNull() {
            // When
            String result = s3StorageService.getPublicUrl("   ");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("given empty key, when get URL, then returns null")
        void givenEmptyKey_whenGetUrl_thenReturnsNull() {
            // When
            String result = s3StorageService.getPublicUrl("");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("given S3Exception during presign in AWS mode, when get URL, then returns null")
        void givenS3ExceptionDuringPresignInAwsMode_whenGetUrl_thenReturnsNull() {
            // Given - create service without endpoint (AWS mode)
            S3StorageService awsService = new S3StorageService(
                    s3Client,
                    s3Presigner,
                    BUCKET_NAME,
                    "", // Empty endpoint = AWS mode
                    MAX_FILE_SIZE,
                    ALLOWED_CONTENT_TYPES
            );

            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenThrow(S3Exception.builder().message("Error").build());

            // When
            String result = awsService.getPublicUrl("pets/123/image.jpg");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("deleteFile")
    class DeleteFile {

        @Test
        @DisplayName("given valid S3 key, when delete, then calls S3 deleteObject")
        void givenValidS3Key_whenDelete_thenCallsS3DeleteObject() {
            // Given
            String s3Key = "pets/123/image.jpg";

            // When
            s3StorageService.deleteFile(s3Key);

            // Then
            ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            verify(s3Client).deleteObject(requestCaptor.capture());
            assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET_NAME);
            assertThat(requestCaptor.getValue().key()).isEqualTo(s3Key);
        }

        @Test
        @DisplayName("given null key, when delete, then does not call S3 and returns gracefully")
        void givenNullKey_whenDelete_thenDoesNotCallS3AndReturnsGracefully() {
            // When
            s3StorageService.deleteFile(null);

            // Then
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("given blank key, when delete, then does not call S3 and returns gracefully")
        void givenBlankKey_whenDelete_thenDoesNotCallS3AndReturnsGracefully() {
            // When
            s3StorageService.deleteFile("   ");

            // Then
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("given S3Exception, when delete, then wraps in StorageException")
        void givenS3Exception_whenDelete_thenWrapsInStorageException() {
            // Given
            String s3Key = "pets/123/image.jpg";
            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                    .errorCode("NotFound")
                    .errorMessage("Not Found")
                    .build();
            S3Exception s3Exception = (S3Exception) S3Exception.builder()
                    .message("Not Found")
                    .statusCode(404)
                    .awsErrorDetails(errorDetails)
                    .build();
            doThrow(s3Exception).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

            // When/Then
            assertThatThrownBy(() -> s3StorageService.deleteFile(s3Key))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasCauseInstanceOf(S3Exception.class);
        }
    }

    @Nested
    @DisplayName("fileExists")
    class FileExists {

        @Test
        @DisplayName("given existing file, when check exists, then returns true")
        void givenExistingFile_whenCheckExists_thenReturnsTrue() {
            // Given
            String s3Key = "pets/123/image.jpg";
            // S3Client.headObject succeeds (no exception)

            // When
            boolean result = s3StorageService.fileExists(s3Key);

            // Then
            assertThat(result).isTrue();
            verify(s3Client).headObject(any(HeadObjectRequest.class));
        }

        @Test
        @DisplayName("given non-existent file, when check exists, then returns false")
        void givenNonExistentFile_whenCheckExists_thenReturnsFalse() {
            // Given
            String s3Key = "pets/123/nonexistent.jpg";
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenThrow(NoSuchKeyException.builder().message("Not found").build());

            // When
            boolean result = s3StorageService.fileExists(s3Key);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("given null key, when check exists, then returns false")
        void givenNullKey_whenCheckExists_thenReturnsFalse() {
            // When
            boolean result = s3StorageService.fileExists(null);

            // Then
            assertThat(result).isFalse();
            verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        }

        @Test
        @DisplayName("given blank key, when check exists, then returns false")
        void givenBlankKey_whenCheckExists_thenReturnsFalse() {
            // When
            boolean result = s3StorageService.fileExists("   ");

            // Then
            assertThat(result).isFalse();
            verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        }

        @Test
        @DisplayName("given S3Exception other than NoSuchKey, when check exists, then returns false")
        void givenS3ExceptionOtherThanNoSuchKey_whenCheckExists_thenReturnsFalse() {
            // Given
            String s3Key = "pets/123/image.jpg";
            S3Exception s3Exception = (S3Exception) S3Exception.builder()
                    .message("Access Denied")
                    .statusCode(403)
                    .build();
            when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Exception);

            // When
            boolean result = s3StorageService.fileExists(s3Key);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getBucketName")
    class GetBucketName {

        @Test
        @DisplayName("when get bucket name, then returns configured bucket name")
        void whenGetBucketName_thenReturnsConfiguredBucketName() {
            // When
            String result = s3StorageService.getBucketName();

            // Then
            assertThat(result).isEqualTo(BUCKET_NAME);
        }
    }

    @Nested
    @DisplayName("File validation")
    class FileValidation {

        @Test
        @DisplayName("given gif file, when upload, then accepts it")
        void givenGifFile_whenUpload_thenAcceptsIt() throws IOException {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("image/gif");
            when(mockFile.getOriginalFilename()).thenReturn("animation.gif");
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // When
            String result = s3StorageService.uploadFile(mockFile, "folder");

            // Then
            assertThat(result).endsWith(".gif");
        }

        @Test
        @DisplayName("given webp file, when upload, then accepts it")
        void givenWebpFile_whenUpload_thenAcceptsIt() throws IOException {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("image/webp");
            when(mockFile.getOriginalFilename()).thenReturn("image.webp");
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // When
            String result = s3StorageService.uploadFile(mockFile, "folder");

            // Then
            assertThat(result).endsWith(".webp");
        }

        @Test
        @DisplayName("given text file, when upload, then rejects it")
        void givenTextFile_whenUpload_thenRejectsIt() {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getContentType()).thenReturn("text/plain");

            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, "folder"))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasMessageContaining("File type not allowed");
        }

        @Test
        @DisplayName("given file at exact max size, when upload, then accepts it")
        void givenFileAtExactMaxSize_whenUpload_thenAcceptsIt() throws IOException {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(MAX_FILE_SIZE); // Exactly 5MB
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getOriginalFilename()).thenReturn("photo.jpg");
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // When
            String result = s3StorageService.uploadFile(mockFile, "folder");

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("given file 1 byte over max size, when upload, then rejects it")
        void givenFile1ByteOverMaxSize_whenUpload_thenRejectsIt() {
            // Given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(MAX_FILE_SIZE + 1); // 1 byte over

            // When/Then
            assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, "folder"))
                    .isInstanceOf(S3StorageService.StorageException.class)
                    .hasMessageContaining("exceeds maximum allowed size");
        }
    }
}
