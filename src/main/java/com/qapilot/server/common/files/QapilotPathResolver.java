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

    public Path qapilotDir() {
        return qapilotDir(defaultTargetRoot());
    }

    public Path qapilotDir(Path targetRoot) {
        return targetRoot.toAbsolutePath().normalize().resolve(".qapilot");
    }

    public Path requireExistingQapilotDir(Path targetRoot) {
        Path qapilotDir = qapilotDir(targetRoot);
        if (!qapilotDir.toFile().isDirectory()) {
            throw new QapilotException(ErrorCode.SYSTEM_001, ".qapilot 경로를 찾을 수 없습니다: " + qapilotDir);
        }
        return qapilotDir;
    }
}
