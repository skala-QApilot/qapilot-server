package com.qapilot.server.organization.persistence;

import com.qapilot.server.organization.domain.OrgMembership;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
public interface OrgMembershipRepository extends JpaRepository<OrgMembership, UUID> {

    List<OrgMembership> findAllByUserId(UUID userId);
}
