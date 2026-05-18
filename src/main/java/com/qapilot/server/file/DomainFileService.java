package com.qapilot.server.file;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.file.domain.DomainFileMeta;
import com.qapilot.server.file.dto.FileUpdateRequest;
import com.qapilot.server.file.store.DomainFileStore;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 도메인 파일 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class DomainFileService {

    private final ServiceDomainService serviceDomainService;
    private final DomainFileStore domainFileStore;

    public DomainFileService(ServiceDomainService serviceDomainService, DomainFileStore domainFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.domainFileStore = domainFileStore;
    }

    public List<DomainFileMeta> list(String serviceId) {
        return domainFileStore.loadAll(qapilotDir(serviceId));
    }

    public DomainFileMeta create(String serviceId, MultipartFile file) {
        return domainFileStore.create(qapilotDir(serviceId), filename(file), bytes(file), contentType(file));
    }

    public DomainFileMeta addVersion(String serviceId, String fileId, MultipartFile file) {
        return domainFileStore.addVersion(qapilotDir(serviceId), fileId, bytes(file), contentType(file));
    }

    public DomainFileMeta update(String serviceId, String fileId, FileUpdateRequest request) {
        return domainFileStore.update(qapilotDir(serviceId), fileId, request.name(), request.reflected());
    }

    public void delete(String serviceId, String fileId) {
        domainFileStore.delete(qapilotDir(serviceId), fileId);
    }

    public List<String> versions(String serviceId, String fileId) {
        return domainFileStore.versions(qapilotDir(serviceId), fileId);
    }

    public Object diff(String serviceId, String fileId) {
        return domainFileStore.diff(qapilotDir(serviceId), fileId);
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }

    private String filename(MultipartFile file) {
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
