package com.qapilot.server.storage;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 클라이언트 빈 — AWS SDK v2.
 *
 * <p>로컬(MinIO) 의 경우 path-style-access=true 필수. AWS 운영은 endpoint 비우고 SDK 기본 사용.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Configuration
public class AwsS3Config {

    private final AwsS3Properties properties;

    public AwsS3Config(AwsS3Properties properties) {
        this.properties = properties;
    }

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(serviceConfiguration());
        applyEndpointOverride(builder::endpointOverride);
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(serviceConfiguration());
        applyEndpointOverride(builder::endpointOverride);
        return builder.build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
        );
    }

    private S3Configuration serviceConfiguration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(properties.pathStyleAccess())
                .build();
    }

    private void applyEndpointOverride(java.util.function.Consumer<URI> setter) {
        String endpoint = properties.endpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            setter.accept(URI.create(endpoint));
        }
    }
}
