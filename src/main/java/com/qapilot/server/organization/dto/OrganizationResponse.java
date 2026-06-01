package com.qapilot.server.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.organization.domain.Organization;
import java.time.Instant;

/**
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
public record OrganizationResponse(
        @JsonProperty("org_id") String orgId,
        String slug,
        String name,
        String plan,
        @JsonProperty("created_at") Instant createdAt
) {

    public static OrganizationResponse from(Organization org) {
        return new OrganizationResponse(
                org.getId().toString(),
                org.getSlug(),
                org.getName(),
                org.getPlan(),
                org.getCreatedAt()
        );
    }
}
