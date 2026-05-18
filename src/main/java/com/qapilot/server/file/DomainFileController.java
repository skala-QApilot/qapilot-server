package com.qapilot.server.file;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.file.domain.DomainFileMeta;
import com.qapilot.server.file.dto.FileUpdateRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 도메인 파일 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/files")
public class DomainFileController {

    private final DomainFileService domainFileService;

    public DomainFileController(DomainFileService domainFileService) {
        this.domainFileService = domainFileService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@PathVariable String serviceId) {
        List<DomainFileMeta> files = domainFileService.list(serviceId);
        return ApiResponse.ok(Map.of("files", files, "count", files.size()));
    }

    @PostMapping
    public ApiResponse<Map<String, DomainFileMeta>> create(
            @PathVariable String serviceId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(Map.of("file", domainFileService.create(serviceId, file)));
    }

    @GetMapping("/{fileId}/versions")
    public ApiResponse<Map<String, Object>> versions(@PathVariable String serviceId, @PathVariable String fileId) {
        return ApiResponse.ok(Map.of("versions", domainFileService.versions(serviceId, fileId)));
    }

    @PostMapping("/{fileId}/versions")
    public ApiResponse<Map<String, DomainFileMeta>> addVersion(
            @PathVariable String serviceId,
            @PathVariable String fileId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(Map.of("file", domainFileService.addVersion(serviceId, fileId, file)));
    }

    @PatchMapping("/{fileId}")
    public ApiResponse<Map<String, DomainFileMeta>> update(
            @PathVariable String serviceId,
            @PathVariable String fileId,
            @RequestBody FileUpdateRequest request
    ) {
        return ApiResponse.ok(Map.of("file", domainFileService.update(serviceId, fileId, request)));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable String serviceId, @PathVariable String fileId) {
        domainFileService.delete(serviceId, fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fileId}/diff")
    public ApiResponse<Map<String, Object>> diff(@PathVariable String serviceId, @PathVariable String fileId) {
        return ApiResponse.ok(Map.of("diff", domainFileService.diff(serviceId, fileId)));
    }
}
