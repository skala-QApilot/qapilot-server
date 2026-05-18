package com.qapilot.server.service.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.common.files.QapilotPathResolver;
import com.qapilot.server.service.domain.ServiceMember;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * members.json 파일 기반 서비스 멤버십 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class MemberFileStore {

    private final JsonFileStore jsonFileStore;
    private final QapilotPathResolver pathResolver;

    public MemberFileStore(JsonFileStore jsonFileStore, QapilotPathResolver pathResolver) {
        this.jsonFileStore = jsonFileStore;
        this.pathResolver = pathResolver;
    }

    public List<ServiceMember> loadMembers() {
        return new ArrayList<>(jsonFileStore.readOrDefault(membersPath(), new TypeReference<>() {
        }, List.of()));
    }

    public void saveMembers(List<ServiceMember> members) {
        jsonFileStore.write(membersPath(), members);
    }

    public Optional<ServiceMember> findByServiceAndUser(String serviceId, String userId) {
        return loadMembers().stream()
                .filter(member -> member.serviceId().equals(serviceId) && member.userId().equals(userId))
                .findFirst();
    }

    public void addIfAbsent(ServiceMember member) {
        List<ServiceMember> members = loadMembers();
        boolean exists = members.stream().anyMatch(existing ->
                existing.serviceId().equals(member.serviceId()) && existing.userId().equals(member.userId()));
        if (!exists) {
            members.add(member);
            saveMembers(members);
        }
    }

    private Path membersPath() {
        return pathResolver.qapilotDir().resolve("members.json");
    }
}
