package com.example.foreverhome.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ses.SesClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.endpoint:#{null}}")
    private String awsEndpoint;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.access-key:#{null}}")
    private String accessKey;

    @Value("${aws.secret-key:#{null}}")
    private String secretKey;

    /**
     * Check if explicit AWS credentials are configured.
     * Returns true if both access key and secret key are provided and non-blank.
     */
    private boolean hasExplicitCredentials() {
        return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    @Bean
    @Profile("!prod")
    public SesClient localSesClient() {
        var builder = SesClient.builder()
                .region(Region.of(region));

        // Use explicit credentials if provided, otherwise fall back to default chain (IAM role)
        if (hasExplicitCredentials()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (awsEndpoint != null && !awsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(awsEndpoint));
        }

        return builder.build();
    }

    @Bean
    @Profile("!prod")
    public S3Client localS3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .forcePathStyle(true);

        // Use explicit credentials if provided, otherwise fall back to default chain (IAM role)
        if (hasExplicitCredentials()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (awsEndpoint != null && !awsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(awsEndpoint));
        }

        return builder.build();
    }

    @Bean
    @Profile("prod")
    public SesClient prodSesClient() {
        return SesClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    @Profile("prod")
    public S3Client prodS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
