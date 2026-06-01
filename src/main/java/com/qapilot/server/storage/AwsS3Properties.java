package com.qapilot.server.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3 설정 — 로컬은 MinIO, 운영은 AWS S3. 코드는 같고 endpoint/credentials 만 다름.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@ConfigurationProperties(prefix = "aws.s3")
public record AwsS3Properties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        Boolean pathStyleAccess
) {

    public AwsS3Properties {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("aws.s3.bucket 필수");
        }
        region = region == null || region.isBlank() ? "us-east-1" : region;
        pathStyleAccess = pathStyleAccess == null ? Boolean.FALSE : pathStyleAccess;
    }
}
