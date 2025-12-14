package com.example.foreverhome.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for storing and retrieving files from S3 (or LocalStack in development).
 *
 * Key design decisions:
 * - Returns S3 keys (not URLs) from upload operations for environment independence
 * - URLs are constructed dynamically based on current endpoint configuration
 * - Supports both LocalStack (dev) and AWS S3 (prod) transparently
 */
@Service
public class S3StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String awsEndpoint;
    private final long maxFileSize;
    private final List<String> allowedContentTypes;

    public S3StorageService(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucketName,
            @Value("${aws.endpoint:}") String awsEndpoint,
            @Value("${aws.s3.max-file-size:5242880}") long maxFileSize,
            @Value("${aws.s3.allowed-content-types:image/jpeg,image/png,image/gif,image/webp}") String allowedContentTypes) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.awsEndpoint = awsEndpoint;
        this.maxFileSize = maxFileSize;
        this.allowedContentTypes = Arrays.asList(allowedContentTypes.split(","));
    }

    /**
     * Uploads a file to S3 and returns the S3 key (not the full URL).
     * The key can be used with {@link #getPublicUrl(String)} to construct a URL.
     *
     * @param file   the file to upload
     * @param folder the folder path within the bucket (e.g., "pets/{petId}")
     * @return the S3 key for the uploaded file
     * @throws StorageException if the upload fails
     */
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String key = folder + "/" + UUID.randomUUID() + extension;

        return uploadFileWithKey(file, key);
    }

    /**
     * Uploads a file to S3 with a specific key (not the full URL).
     * The key can be used with {@link #getPublicUrl(String)} to construct a URL.
     *
     * @param file the file to upload
     * @param key  the full S3 key (e.g., "pets/CHIP123/1.jpg")
     * @return the S3 key for the uploaded file
     * @throws StorageException if the upload fails
     */
    public String uploadFileWithKey(MultipartFile file, String key) {
        validateFile(file);

        try {
            logger.info("Uploading file to S3: bucket={}, key={}, contentType={}, size={}",
                    bucketName, key, file.getContentType(), file.getSize());

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            logger.info("Successfully uploaded file to S3: key={}", key);
            return key;
        } catch (S3Exception e) {
            logger.error("S3 error uploading file: statusCode={}, awsErrorCode={}, message={}",
                    e.statusCode(), e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (IOException e) {
            logger.error("IO error uploading file", e);
            throw new StorageException("Failed to upload file", e);
        }
    }

    /**
     * Deletes a file from S3 using its key.
     *
     * @param s3Key the S3 key of the file to delete
     */
    public void deleteFile(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            logger.warn("Attempted to delete file with null or blank key");
            return;
        }

        try {
            logger.info("Deleting file from S3: bucket={}, key={}", bucketName, s3Key);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            logger.info("Successfully deleted file from S3: key={}", s3Key);
        } catch (S3Exception e) {
            logger.error("S3 error deleting file: key={}, statusCode={}, message={}",
                    s3Key, e.statusCode(), e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to delete file from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Constructs a public URL for an S3 key based on the current environment configuration.
     *
     * @param s3Key the S3 key
     * @return the public URL for accessing the file
     */
    public String getPublicUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }

        if (awsEndpoint != null && !awsEndpoint.isBlank()) {
            // LocalStack URL format: http://localhost:4566/bucket/key
            return awsEndpoint + "/" + bucketName + "/" + s3Key;
        }
        // AWS S3 URL format: https://bucket.s3.amazonaws.com/key
        return "https://" + bucketName + ".s3.amazonaws.com/" + s3Key;
    }

    /**
     * Checks if a file exists in S3.
     *
     * @param s3Key the S3 key to check
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return false;
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            logger.warn("Error checking if file exists: key={}, error={}", s3Key, e.getMessage());
            return false;
        }
    }

    /**
     * Gets the bucket name being used.
     *
     * @return the S3 bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new StorageException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new StorageException("File type not allowed. Allowed types: " + String.join(", ", allowedContentTypes));
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public static class StorageException extends RuntimeException {
        public StorageException(String message) {
            super(message);
        }

        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
