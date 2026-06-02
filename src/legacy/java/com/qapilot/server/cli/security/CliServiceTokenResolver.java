package com.qapilot.server.cli.security;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.service.store.ServiceFileStore;
import org.springframework.stereotype.Component;

/**
 * CLI Authorization Bearer 토큰으로 서비스를 식별한다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class CliServiceTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ServiceFileStore serviceFileStore;

    public CliServiceTokenResolver(ServiceFileStore serviceFileStore) {
        this.serviceFileStore = serviceFileStore;
    }

    public QapilotService resolve(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new QapilotException(ErrorCode.CLI_SYNC_001);
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new QapilotException(ErrorCode.CLI_SYNC_001);
        }
        return serviceFileStore.findByServerAuthToken(token)
                .orElseThrow(() -> new QapilotException(ErrorCode.CLI_SYNC_001));
    }
}
