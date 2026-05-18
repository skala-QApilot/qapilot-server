package com.qapilot.server.file.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 도메인 파일 메타데이터.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record DomainFileMeta(
        @JsonProperty("file_id") String fileId,
        String name,
        String version,
        @JsonProperty("version_number") int versionNumber,
        boolean reflected,
        @JsonProperty("uploaded_at") String uploadedAt,
        @JsonProperty("size_bytes") long sizeBytes,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("storage_path") String storagePath
) {
}
