package com.qapilot.server.organization;

import com.qapilot.server.organization.persistence.OrganizationRepository;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * organization 표시 이름에서 globally-unique slug 생성.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Component
public class OrgSlugGenerator {

    private final OrganizationRepository repository;

    public OrgSlugGenerator(OrganizationRepository repository) {
        this.repository = repository;
    }

    public String generate(String displayName) {
        String base = normalize(displayName);
        if (!repository.existsBySlug(base)) {
            return base;
        }
        int suffix = 2;
        while (repository.existsBySlug(base + "-" + suffix)) {
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
        return normalized.isBlank() ? "workspace" : normalized;
    }
}
