package com.qapilot.server.storage;

import java.net.URI;
import java.time.Duration;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 객체 저장소 래퍼.
 *
 * <p>스크린샷·생성 코드·도메인 문서·코드베이스 인덱스 등 blob/대용량 텍스트의 단일 진입점.
 * DB row 에는 {@code s3_key} 만 보관하고, 실제 바이트는 여기 통해 입출력한다.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, AwsS3Properties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = properties.bucket();
    }

    /** 버킷 자체 접근 가능 여부 — 헬스 체크용. */
    public boolean bucketAccessible() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String put(String key, byte[] bytes, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes)
        );
        return key;
    }

    public byte[] get(String key) {
        return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(key).build()
        ).asByteArray();
    }

    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /** UI 가 직접 GET 할 수 있는 임시 URL. TTL 짧게 (분~시간 단위) 권장. */
    public URI presignedGet(String key, Duration ttl) {
        return URI.create(
                s3Presigner.presignGetObject(
                        GetObjectPresignRequest.builder()
                                .signatureDuration(ttl)
                                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                                .build()
                ).url().toString()
        );
    }

    /** 클라이언트가 직접 PUT 업로드할 수 있는 임시 URL. */
    public URI presignedPut(String key, Duration ttl, String contentType) {
        return URI.create(
                s3Presigner.presignPutObject(
                        PutObjectPresignRequest.builder()
                                .signatureDuration(ttl)
                                .putObjectRequest(PutObjectRequest.builder()
                                        .bucket(bucket)
                                        .key(key)
                                        .contentType(contentType)
                                        .build())
                                .build()
                ).url().toString()
        );
    }

    public String bucket() {
        return bucket;
    }
}
