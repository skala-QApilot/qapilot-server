package com.qapilot.server.file.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * domain_documents 테이블. PR-15h.
 *
 * <p>한 행 = 한 (file, version). 같은 file 의 여러 버전은 같은 file_id 공유.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
@Entity
@Table(name = "domain_documents")
@Getter
@Setter
@NoArgsConstructor
public class DomainDocumentEntity {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private int version;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    private Long bytes;

    private String sha256;

    @Column(nullable = false)
    private boolean reflected;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }
}
