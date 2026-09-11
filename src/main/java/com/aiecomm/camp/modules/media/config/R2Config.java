package com.aiecomm.camp.modules.media.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@Slf4j
public class R2Config {

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Value("${cloudflare.r2.access-key:}")
    private String accessKey;

    @Value("${cloudflare.r2.secret-key:}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        String effectiveAccessKey = (accessKey != null && !accessKey.isBlank()) ? accessKey : "placeholder-access-key";
        String effectiveSecretKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : "placeholder-secret-key";

        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            log.warn("Cloudflare R2 access-key or secret-key is missing in environment variables. S3Client initialized with placeholder credentials.");
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(effectiveAccessKey, effectiveSecretKey);

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        String effectiveAccessKey = (accessKey != null && !accessKey.isBlank()) ? accessKey : "placeholder-access-key";
        String effectiveSecretKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : "placeholder-secret-key";

        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            log.warn("Cloudflare R2 access-key or secret-key is missing in environment variables. S3Presigner initialized with placeholder credentials.");
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(effectiveAccessKey, effectiveSecretKey);

        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
