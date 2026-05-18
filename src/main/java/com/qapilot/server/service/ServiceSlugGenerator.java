package com.qapilot.server.service;

import com.qapilot.server.service.domain.QapilotService;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 서비스 표시 이름에서 URL용 project_slug를 생성한다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class ServiceSlugGenerator {

    public String generate(String name, List<QapilotService> services) {
        String base = normalize(name);
        Set<String> existing = services.stream().map(QapilotService::projectSlug).collect(Collectors.toSet());
        if (!existing.contains(base)) {
            return base;
        }
        int suffix = 2;
        while (existing.contains(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    private String normalize(String name) {
        String normalized = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return normalized.isBlank() ? "service" : normalized;
    }
}
