package com.qapilot.server.common.files;

import com.qapilot.server.common.config.QapilotProperties;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * 대상 테스트 프로젝트의 .qapilot 경로를 해석한다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class QapilotPathResolver {

    private final QapilotProperties properties;

    public QapilotPathResolver(QapilotProperties properties) {
        this.properties = properties;
    }

    public Path defaultTargetRoot() {
        return Path.of(properties.storage().defaultTargetRoot()).toAbsolutePath().normalize();
    }

    /** 전역 .qapilot 경로 (services.json / users.json / auth/ 보관). */
    public Path qapilotDir() {
        return qapilotDir(defaultTargetRoot());
    }

    /** 전역 .qapilot — root .qapilot 디렉토리 (slug 미지정). */
    public Path qapilotDir(Path targetRoot) {
        return targetRoot.toAbsolutePath().normalize().resolve(".qapilot");
    }

    /**
     * 서비스별 격리된 디렉토리 — `<root>/.qapilot/<slug>`.
     *
     * <p>같은 target_root 안에서 여러 서비스가 동시에 시나리오/trace/결과/RTM 등을
     * 보관하더라도 디렉토리가 분리되어 데이터가 섞이지 않도록 한다.
     * services.json, users.json, auth/ 는 전역 (qapilotDir(targetRoot)) 에 그대로 둔다.
     */
    public Path qapilotDir(Path targetRoot, String slug) {
        if (slug == null || slug.isBlank()) {
            return qapilotDir(targetRoot);
        }
        return qapilotDir(targetRoot).resolve(slug);
    }

    public Path requireExistingQapilotDir(Path targetRoot) {
        Path qapilotDir = qapilotDir(targetRoot);
        if (!qapilotDir.toFile().isDirectory()) {
            throw new QapilotException(ErrorCode.SYSTEM_001, ".qapilot 경로를 찾을 수 없습니다: " + qapilotDir);
        }
        return qapilotDir;
    }
}
