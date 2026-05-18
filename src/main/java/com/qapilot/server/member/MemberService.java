package com.qapilot.server.member;

import com.qapilot.server.auth.domain.UserAccount;
import com.qapilot.server.auth.store.UserFileStore;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.member.domain.Member;
import com.qapilot.server.member.dto.InviteMemberRequest;
import com.qapilot.server.member.dto.MemberResponse;
import com.qapilot.server.member.dto.UpdateMemberRequest;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 멤버 관리 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class MemberService {

    private final ServiceDomainService serviceDomainService;
    private final MemberFileStore memberFileStore;
    private final UserFileStore userFileStore;

    public MemberService(
            ServiceDomainService serviceDomainService,
            MemberFileStore memberFileStore,
            UserFileStore userFileStore
    ) {
        this.serviceDomainService = serviceDomainService;
        this.memberFileStore = memberFileStore;
        this.userFileStore = userFileStore;
    }

    public List<MemberResponse> list(String serviceId) {
        Path qapilotDir = qapilotDir(serviceId);
        return memberFileStore.load(qapilotDir).stream()
                .filter(m -> serviceId.equals(m.serviceId()))
                .map(m -> MemberResponse.of(m, userFileStore.findById(m.userId()).orElse(null)))
                .toList();
    }

    public MemberResponse invite(String serviceId, InviteMemberRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "email 필드가 필요합니다.");
        }

        Path qapilotDir = qapilotDir(serviceId);
        Optional<UserAccount> existingUser = userFileStore.findByEmail(request.email());

        String userId = existingUser.map(UserAccount::userId).orElseGet(() -> UUID.randomUUID().toString());

        // 이미 초대된 이메일이면 기존 멤버 반환
        Optional<Member> existingMember = memberFileStore.findByServiceAndUserId(qapilotDir, serviceId, userId);
        if (existingMember.isPresent()) {
            UserAccount user = userFileStore.findById(userId).orElse(null);
            return MemberResponse.of(existingMember.get(), user);
        }

        String role = request.role() != null ? request.role() : "member";
        Member member = new Member(
                UUID.randomUUID().toString(),
                serviceId,
                userId,
                role,
                request.team(),
                "active",
                Instant.now().toString()
        );

        List<Member> members = memberFileStore.load(qapilotDir);
        members.add(member);
        memberFileStore.save(qapilotDir, members);

        // users.json에 없으면 초대 요청의 email/name을 응답에 사용
        UserAccount user = existingUser.orElse(
                new UserAccount(userId, request.email(), null, request.name(), null, null)
        );
        return MemberResponse.of(member, user);
    }

    public String exportCsv(String serviceId) {
        List<MemberResponse> members = list(serviceId);
        StringBuilder csv = new StringBuilder("memberId,name,email,role,team,joinedAt\n");
        for (MemberResponse m : members) {
            csv.append(csvField(m.memberId())).append(",")
                    .append(csvField(m.name())).append(",")
                    .append(csvField(m.email())).append(",")
                    .append(csvField(m.role())).append(",")
                    .append(csvField(m.team())).append(",")
                    .append(csvField(m.joinedAt())).append("\n");
        }
        return csv.toString();
    }

    public MemberResponse update(String serviceId, String memberId, UpdateMemberRequest request) {
        Path qapilotDir = qapilotDir(serviceId);
        List<Member> members = memberFileStore.load(qapilotDir);

        Member current = members.stream()
                .filter(m -> memberId.equals(m.memberId()))
                .findFirst()
                .orElseThrow(() -> new QapilotException(ErrorCode.MEMBER_001));

        Member updated = new Member(
                current.memberId(),
                current.serviceId(),
                current.userId(),
                request.role() != null ? request.role() : current.role(),
                request.team() != null ? request.team() : current.team(),
                current.status(),
                current.joinedAt()
        );

        List<Member> replaced = members.stream()
                .map(m -> memberId.equals(m.memberId()) ? updated : m)
                .toList();
        memberFileStore.save(qapilotDir, replaced);

        UserAccount user = userFileStore.findById(updated.userId()).orElse(null);
        return MemberResponse.of(updated, user);
    }

    public void delete(String serviceId, String memberId) {
        Path qapilotDir = qapilotDir(serviceId);
        List<Member> members = memberFileStore.load(qapilotDir);

        boolean exists = members.stream().anyMatch(m -> memberId.equals(m.memberId()));
        if (!exists) {
            throw new QapilotException(ErrorCode.MEMBER_001);
        }

        List<Member> filtered = members.stream()
                .filter(m -> !memberId.equals(m.memberId()))
                .toList();
        memberFileStore.save(qapilotDir, filtered);
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
