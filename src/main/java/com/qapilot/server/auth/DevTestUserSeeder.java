package com.qapilot.server.auth;

import com.qapilot.server.auth.domain.UserAccount;
import com.qapilot.server.auth.store.UserFileStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * dev 프로필 전용 — 로컬 개발 시 admin 계정 자동 시드.
 *
 * <p>이미 동일 이메일의 사용자가 있으면 시드하지 않는다 (중복 방지).
 * 프로덕션(default) 프로필에서는 본 빈이 생성되지 않으므로 동작하지 않는다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
@Component
@Profile("dev")
public class DevTestUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevTestUserSeeder.class);

    private final UserFileStore userFileStore;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public DevTestUserSeeder(
            UserFileStore userFileStore,
            PasswordEncoder passwordEncoder,
            @Value("${qapilot.dev.seed-admin.email:admin@qapilot.com}") String email,
            @Value("${qapilot.dev.seed-admin.password:admin1234}") String password,
            @Value("${qapilot.dev.seed-admin.name:테스트 관리자}") String name
    ) {
        this.userFileStore = userFileStore;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        String normalized = email.toLowerCase();
        if (userFileStore.findByEmail(normalized).isPresent()) {
            log.info("Dev seed admin already exists, skipping: {}", normalized);
            return;
        }

        UserAccount admin = new UserAccount(
                UUID.randomUUID().toString(),
                normalized,
                passwordEncoder.encode(password),
                name,
                "admin",
                Instant.now().toString()
        );
        List<UserAccount> updated = new ArrayList<>(userFileStore.loadUsers());
        updated.add(admin);
        userFileStore.saveUsers(updated);
        log.info("Seeded dev test user: {} ({})", normalized, admin.userId());
    }
}
