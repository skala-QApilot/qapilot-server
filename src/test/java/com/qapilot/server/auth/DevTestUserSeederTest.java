package com.qapilot.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qapilot.server.auth.domain.UserAccount;
import com.qapilot.server.auth.store.UserFileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * DevTestUserSeeder 동작 검증.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
class DevTestUserSeederTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("dev")
    @AutoConfigureMockMvc
    class WhenDevProfileActive {

        private static Path targetRoot;

        @Autowired
        private UserFileStore userFileStore;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private MockMvc mockMvc;

        @DynamicPropertySource
        static void properties(DynamicPropertyRegistry registry) throws Exception {
            targetRoot = Files.createTempDirectory("qapilot-dev-seed-active");
            registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
        }

        @Test
        void seedsAdminAndAllowsLogin() throws Exception {
            UserAccount admin = userFileStore.findByEmail("admin@qapilot.com").orElseThrow();
            assertThat(admin.role()).isEqualTo("admin");
            assertThat(admin.name()).isEqualTo("테스트 관리자");
            assertThat(passwordEncoder.matches("admin1234", admin.hashedPassword())).isTrue();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"admin@qapilot.com\",\"password\":\"admin1234\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").exists());
        }
    }

    @Nested
    @SpringBootTest
    class WhenDefaultProfileActive {

        private static Path targetRoot;

        @Autowired
        private UserFileStore userFileStore;

        @Autowired
        private ApplicationContext applicationContext;

        @DynamicPropertySource
        static void properties(DynamicPropertyRegistry registry) throws Exception {
            targetRoot = Files.createTempDirectory("qapilot-dev-seed-inactive");
            registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
        }

        @BeforeEach
        void clean() throws Exception {
            Path qapilotDir = targetRoot.resolve(".qapilot");
            if (Files.exists(qapilotDir)) {
                try (var stream = Files.walk(qapilotDir)) {
                    for (Path item : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                        Files.deleteIfExists(item);
                    }
                }
            }
        }

        @Test
        void doesNotSeedAdmin() {
            assertThat(applicationContext.getBeansOfType(DevTestUserSeeder.class)).isEmpty();
            assertThat(userFileStore.findByEmail("admin@qapilot.com")).isEmpty();
        }
    }
}
