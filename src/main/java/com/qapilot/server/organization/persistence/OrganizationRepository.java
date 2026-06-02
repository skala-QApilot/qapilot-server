package com.qapilot.server.organization.persistence;

import com.qapilot.server.organization.domain.Organization;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsBySlug(String slug);

    List<Organization> findAllByIdIn(List<UUID> ids);
}
