package com.example.foreverhome.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class S3StorageService {

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

    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String key = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return buildFileUrl(key);
        } catch (IOException e) {
            throw new StorageException("Failed to upload file", e);
        }
    }

    public void deleteFile(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        if (key == null) {
            return;
        }

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteRequest);
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

    private String buildFileUrl(String key) {
        if (awsEndpoint != null && !awsEndpoint.isBlank()) {
            // LocalStack URL format
            return awsEndpoint + "/" + bucketName + "/" + key;
        }
        // AWS S3 URL format
        return "https://" + bucketName + ".s3.amazonaws.com/" + key;
    }

    private String extractKeyFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        // Handle LocalStack format: http://localhost:4566/bucket/key
        if (url.contains(bucketName + "/")) {
            int bucketIndex = url.indexOf(bucketName + "/");
            return url.substring(bucketIndex + bucketName.length() + 1);
        }

        // Handle AWS S3 format: https://bucket.s3.amazonaws.com/key
        if (url.contains(".s3.amazonaws.com/")) {
            int keyIndex = url.indexOf(".s3.amazonaws.com/") + ".s3.amazonaws.com/".length();
            return url.substring(keyIndex);
        }

        return null;
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
