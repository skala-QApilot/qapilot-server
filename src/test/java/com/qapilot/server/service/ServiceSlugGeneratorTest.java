package com.qapilot.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.qapilot.server.service.domain.QapilotService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * ServiceSlugGenerator 단위 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
class ServiceSlugGeneratorTest {

    private final ServiceSlugGenerator generator = new ServiceSlugGenerator();

    @Test
    void generateSlugFromServiceName() {
        assertThat(generator.generate("System Under Test", List.of())).isEqualTo("system-under-test");
    }

    @Test
    void generateSlugWithNumericSuffixWhenDuplicate() {
        QapilotService existing = new QapilotService(
                "svc-1", "system-under-test", "System Under Test", "",
                "/tmp/sut", "/tmp/sut/.qapilot", "http://localhost:8080/system-under-test",
                "token", "now", null, "now", "now"
        );

        assertThat(generator.generate("System Under Test", List.of(existing))).isEqualTo("system-under-test-2");
    }
}
