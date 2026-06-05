package com.qapilot.server.file;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.file.domain.DomainFileMeta;
import com.qapilot.server.file.dto.FileUpdateRequest;
import com.qapilot.server.file.persistence.DomainDocumentEntity;
import com.qapilot.server.file.persistence.DomainDocumentRepository;
import com.qapilot.server.storage.S3Service;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 도메인 파일 유스케이스. S3 + DB 가 진실의 원천이다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18, dual-write 적용 2026-06-02
 */
@Service
public class DomainFileService {

    private final DomainDocumentRepository documentRepository;
    private final S3Service s3Service;

    public DomainFileService(
            DomainDocumentRepository documentRepository,
            S3Service s3Service
    ) {
        this.documentRepository = documentRepository;
        this.s3Service = s3Service;
    }

    public List<DomainFileMeta> list(String serviceId) {
        UUID svc = UUID.fromString(serviceId);
        return documentRepository.findAllByServiceIdOrderByUploadedAtDesc(svc).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> e.getFileId() != null ? e.getFileId() : e.getId()))
                .values().stream()
                .map(group -> group.stream().max(Comparator.comparingInt(DomainDocumentEntity::getVersion)).orElseThrow())
                .sorted(Comparator.comparing(DomainDocumentEntity::getUploadedAt).reversed())
                .map(this::toMeta)
                .toList();
    }

    public DomainFileMeta create(String serviceId, MultipartFile file) {
        UUID svc = UUID.fromString(serviceId);
        UUID fileId = UUID.randomUUID();
        String filename = requireFilename(file);
        byte[] payload = bytes(file);
        String s3Key = buildKey(svc, fileId, 1, filename);
        if (s3Service.put(s3Key, payload, contentType(file)) == null) {
            throw new QapilotException(ErrorCode.FILE_001, "S3 업로드 실패");
        }
        DomainDocumentEntity entity = save(svc, fileId, filename, 1, s3Key, file);
        return toMeta(entity);
    }

    public DomainFileMeta addVersion(String serviceId, String fileId, MultipartFile file) {
        UUID svc = UUID.fromString(serviceId);
        UUID fid = UUID.fromString(fileId);
        DomainDocumentEntity latest = documentRepository.findFirstByFileIdOrderByVersionDesc(fid)
                .orElseThrow(() -> new QapilotException(ErrorCode.FILE_001, "원본 파일을 찾을 수 없습니다."));
        int nextVersion = latest.getVersion() + 1;
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : latest.getFilename();
        byte[] payload = bytes(file);
        String s3Key = buildKey(svc, fid, nextVersion, filename);
        s3Service.put(s3Key, payload, contentType(file));
        DomainDocumentEntity entity = save(svc, fid, filename, nextVersion, s3Key, file);
        return toMeta(entity);
    }

    public DomainFileMeta update(String serviceId, String fileId, FileUpdateRequest request) {
        UUID fid = UUID.fromString(fileId);
        DomainDocumentEntity latest = documentRepository.findFirstByFileIdOrderByVersionDesc(fid)
                .orElseThrow(() -> new QapilotException(ErrorCode.FILE_001));
        if (request.name() != null && !request.name().isBlank()) {
            latest.setFilename(request.name());
        }
        if (request.reflected() != null) {
            latest.setReflected(request.reflected());
        }
        documentRepository.save(latest);
        return toMeta(latest);
    }

    public void delete(String serviceId, String fileId) {
        UUID fid = UUID.fromString(fileId);
        List<DomainDocumentEntity> versions = documentRepository.findAllByFileIdOrderByVersionDesc(fid);
        if (versions.isEmpty()) {
            throw new QapilotException(ErrorCode.FILE_001);
        }
        documentRepository.deleteAll(versions);
    }

    public List<String> versions(String serviceId, String fileId) {
        UUID fid = UUID.fromString(fileId);
        return documentRepository.findAllByFileIdOrderByVersionDesc(fid).stream()
                .map(e -> "v" + e.getVersion())
                .toList();
    }

    public Object diff(String serviceId, String fileId) {
        // Phase 2 — 두 버전 사이의 diff. 현재는 빈 객체로 반환.
        return java.util.Map.of("file_id", fileId, "diffs", List.of());
    }

    private DomainDocumentEntity save(UUID serviceId, UUID fileId, String filename, int version,
                                       String s3Key, MultipartFile file) {
        DomainDocumentEntity entity = new DomainDocumentEntity();
        entity.setId(UUID.randomUUID());
        entity.setServiceId(serviceId);
        entity.setFileId(fileId);
        entity.setFilename(filename);
        entity.setVersion(version);
        entity.setS3Key(s3Key);
        entity.setBytes(file.getSize());
        entity.setMimeType(contentType(file));
        documentRepository.save(entity);
        return entity;
    }

    private DomainFileMeta toMeta(DomainDocumentEntity e) {
        return new DomainFileMeta(
                e.getFileId() != null ? e.getFileId().toString() : e.getId().toString(),
                e.getFilename(),
                "v" + e.getVersion(),
                e.getVersion(),
                e.isReflected(),
                e.getUploadedAt() == null ? null : e.getUploadedAt().toString(),
                e.getBytes() == null ? 0L : e.getBytes(),
                e.getMimeType(),
                e.getS3Key()
        );
    }

    private String buildKey(UUID serviceId, UUID fileId, int version, String filename) {
        return String.format("services/%s/domain/%s/v%d/%s", serviceId, fileId, version, filename);
    }

    private String requireFilename(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "파일명이 필요합니다.");
        }
        return name;
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "업로드 파일을 읽을 수 없습니다.");
        }
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }
}
