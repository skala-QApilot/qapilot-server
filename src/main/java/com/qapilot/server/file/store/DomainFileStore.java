package com.qapilot.server.file.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.file.domain.DomainFileMeta;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * .qapilot/domain 파일 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class DomainFileStore {

    private final JsonFileStore jsonFileStore;

    public DomainFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<DomainFileMeta> loadAll(Path qapilotDir) {
        return new ArrayList<>(jsonFileStore.readOrDefault(metaPath(qapilotDir), new TypeReference<>() {
        }, List.of()));
    }

    public DomainFileMeta create(Path qapilotDir, String name, byte[] content, String mimeType) {
        String fileId = UUID.randomUUID().toString();
        DomainFileMeta meta = writeVersion(qapilotDir, fileId, name, content, mimeType, 1, false);
        List<DomainFileMeta> files = loadAll(qapilotDir);
        files.add(meta);
        saveMeta(qapilotDir, files);
        return meta;
    }

    public DomainFileMeta addVersion(Path qapilotDir, String fileId, byte[] content, String mimeType) {
        DomainFileMeta current = get(qapilotDir, fileId);
        DomainFileMeta updated = writeVersion(
                qapilotDir, fileId, current.name(), content, mimeType, current.versionNumber() + 1, current.reflected()
        );
        saveMeta(qapilotDir, replace(loadAll(qapilotDir), updated));
        return updated;
    }

    public DomainFileMeta update(Path qapilotDir, String fileId, String name, Boolean reflected) {
        DomainFileMeta current = get(qapilotDir, fileId);
        DomainFileMeta updated = new DomainFileMeta(
                current.fileId(),
                name == null || name.isBlank() ? current.name() : name.trim(),
                current.version(),
                current.versionNumber(),
                reflected == null ? current.reflected() : reflected,
                current.uploadedAt(),
                current.sizeBytes(),
                current.mimeType(),
                current.storagePath()
        );
        saveMeta(qapilotDir, replace(loadAll(qapilotDir), updated));
        return updated;
    }

    public void delete(Path qapilotDir, String fileId) {
        get(qapilotDir, fileId);
        saveMeta(qapilotDir, loadAll(qapilotDir).stream()
                .filter(file -> !file.fileId().equals(fileId))
                .toList());
        deleteDirectory(domainDir(qapilotDir).resolve(fileId));
    }

    public DomainFileMeta get(Path qapilotDir, String fileId) {
        return find(qapilotDir, fileId).orElseThrow(() -> new QapilotException(ErrorCode.FILE_002));
    }

    public List<String> versions(Path qapilotDir, String fileId) {
        get(qapilotDir, fileId);
        Path fileDir = domainDir(qapilotDir).resolve(fileId);
        if (!Files.isDirectory(fileDir)) {
            return List.of();
        }
        try (var stream = Files.list(fileDir)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "파일 버전 목록을 읽을 수 없습니다.");
        }
    }

    public Object diff(Path qapilotDir, String fileId) {
        List<String> versions = versions(qapilotDir, fileId);
        if (versions.size() <= 1) {
            return java.util.Map.of("type", "initial", "message", "최초 버전");
        }
        return java.util.Map.of("type", "unsupported", "message", "MVP에서는 파일 diff 상세를 지원하지 않습니다.");
    }

    private Optional<DomainFileMeta> find(Path qapilotDir, String fileId) {
        return loadAll(qapilotDir).stream().filter(file -> file.fileId().equals(fileId)).findFirst();
    }

    private DomainFileMeta writeVersion(
            Path qapilotDir,
            String fileId,
            String name,
            byte[] content,
            String mimeType,
            int versionNumber,
            boolean reflected
    ) {
        String safeName = name.replaceAll("[/\\\\]", "_");
        String version = "v" + versionNumber;
        Path path = domainDir(qapilotDir).resolve(fileId).resolve(version + "_" + safeName);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "파일을 저장할 수 없습니다.");
        }
        return new DomainFileMeta(fileId, name, version, versionNumber, reflected,
                Instant.now().toString(), content.length, mimeType, path.toString());
    }

    private void saveMeta(Path qapilotDir, List<DomainFileMeta> files) {
        jsonFileStore.write(metaPath(qapilotDir), files.stream()
                .sorted(Comparator.comparing(DomainFileMeta::uploadedAt).reversed())
                .toList());
    }

    private List<DomainFileMeta> replace(List<DomainFileMeta> files, DomainFileMeta updated) {
        return files.stream()
                .map(file -> file.fileId().equals(updated.fileId()) ? updated : file)
                .toList();
    }

    private void deleteDirectory(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            for (Path path : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "파일 디렉터리를 삭제할 수 없습니다.");
        }
    }

    private Path metaPath(Path qapilotDir) {
        return domainDir(qapilotDir).resolve("files.json");
    }

    private Path domainDir(Path qapilotDir) {
        return qapilotDir.resolve("domain");
    }
}
