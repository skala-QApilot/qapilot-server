package com.qapilot.server.storage;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * S3 헬스 체크 — 부팅 직후 버킷 접근 여부 검증용.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@RestController
@RequestMapping("/health")
public class StorageHealthController {

    private final S3Service s3Service;

    public StorageHealthController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/storage")
    public ResponseEntity<Map<String, Object>> storage() {
        boolean accessible = s3Service.bucketAccessible();
        Map<String, Object> body = Map.of(
                "bucket", s3Service.bucket(),
                "accessible", accessible
        );
        return ResponseEntity.status(accessible ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
