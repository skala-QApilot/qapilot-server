package com.qapilot.server.auth;

import com.qapilot.server.auth.persistence.UserEntity;
import com.qapilot.server.auth.persistence.UserRepository;
import com.qapilot.server.organization.OrganizationService;
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
 * PR-15h — file 기반 UserFileStore 제거, JPA only.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
@Component
@Profile("dev")
public class DevTestUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevTestUserSeeder.class);

    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public DevTestUserSeeder(
            UserRepository userRepository,
            OrganizationService organizationService,
            PasswordEncoder passwordEncoder,
            @Value("${qapilot.dev.seed-admin.email:admin@qapilot.com}") String email,
            @Value("${qapilot.dev.seed-admin.password:admin1234}") String password,
            @Value("${qapilot.dev.seed-admin.name:테스트 관리자}") String name
    ) {
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        String normalized = email.toLowerCase();
        if (userRepository.findByEmail(normalized).isPresent()) {
            log.info("Dev seed admin already exists, skipping: {}", normalized);
            return;
        }

        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setEmail(normalized);
        entity.setHashedPassword(passwordEncoder.encode(password));
        entity.setName(name);
        entity.setRole("admin");
        userRepository.save(entity);
        organizationService.createPersonalOrg(userId, name);
        log.info("Seeded dev test user: {} ({})", normalized, userId);
    }
}
