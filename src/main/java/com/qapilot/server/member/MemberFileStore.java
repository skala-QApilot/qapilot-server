package com.qapilot.server.member;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.member.domain.Member;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 서비스 범위 members.json 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component("memberDomainFileStore")
public class MemberFileStore {

    private final JsonFileStore jsonFileStore;

    public MemberFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<Member> load(Path qapilotDir) {
        return new ArrayList<>(jsonFileStore.readOrDefault(membersPath(qapilotDir), new TypeReference<>() {
        }, List.of()));
    }

    public void save(Path qapilotDir, List<Member> members) {
        jsonFileStore.write(membersPath(qapilotDir), members);
    }

    public Optional<Member> findById(Path qapilotDir, String memberId) {
        return load(qapilotDir).stream()
                .filter(m -> memberId.equals(m.memberId()))
                .findFirst();
    }

    public Optional<Member> findByServiceAndUserId(Path qapilotDir, String serviceId, String userId) {
        return load(qapilotDir).stream()
                .filter(m -> serviceId.equals(m.serviceId()) && userId.equals(m.userId()))
                .findFirst();
    }

    private Path membersPath(Path qapilotDir) {
        return qapilotDir.resolve("members.json");
    }
}
