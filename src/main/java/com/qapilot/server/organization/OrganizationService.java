package com.qapilot.server.organization;

import com.qapilot.server.organization.domain.OrgMembership;
import com.qapilot.server.organization.domain.Organization;
import com.qapilot.server.organization.persistence.OrgMembershipRepository;
import com.qapilot.server.organization.persistence.OrganizationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Organization / OrgMembership 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository orgMembershipRepository;
    private final OrgSlugGenerator slugGenerator;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrgMembershipRepository orgMembershipRepository,
            OrgSlugGenerator slugGenerator
    ) {
        this.organizationRepository = organizationRepository;
        this.orgMembershipRepository = orgMembershipRepository;
        this.slugGenerator = slugGenerator;
    }

    /** 신규 사용자 가입 시 호출 — personal org 자동 생성 + owner 멤버십 부여. */
    @Transactional
    public Organization createPersonalOrg(UUID userId, String userName) {
        String displayName = (userName == null || userName.isBlank())
                ? "내 워크스페이스"
                : userName + "의 워크스페이스";

        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setSlug(slugGenerator.generate(userName));
        org.setName(displayName);
        org.setPlan("free");
        organizationRepository.save(org);

        OrgMembership membership = new OrgMembership();
        membership.setId(UUID.randomUUID());
        membership.setOrgId(org.getId());
        membership.setUserId(userId);
        membership.setRole("owner");
        membership.setJoinedAt(Instant.now());
        orgMembershipRepository.save(membership);

        return org;
    }

    @Transactional(readOnly = true)
    public List<Organization> listForUser(UUID userId) {
        List<UUID> orgIds = orgMembershipRepository.findAllByUserId(userId).stream()
                .map(OrgMembership::getOrgId)
                .toList();
        if (orgIds.isEmpty()) {
            return List.of();
        }
        return organizationRepository.findAllByIdIn(orgIds);
    }
}
