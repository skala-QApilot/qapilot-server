package com.qapilot.server.auth.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.auth.domain.UserAccount;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.common.files.QapilotPathResolver;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * users.json 파일 기반 사용자 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class UserFileStore {

    private final JsonFileStore jsonFileStore;
    private final QapilotPathResolver pathResolver;

    public UserFileStore(JsonFileStore jsonFileStore, QapilotPathResolver pathResolver) {
        this.jsonFileStore = jsonFileStore;
        this.pathResolver = pathResolver;
    }

    public List<UserAccount> loadUsers() {
        return new ArrayList<>(jsonFileStore.readOrDefault(usersPath(), new TypeReference<>() {
        }, List.of()));
    }

    public void saveUsers(List<UserAccount> users) {
        jsonFileStore.write(usersPath(), users);
    }

    public Optional<UserAccount> findByEmail(String email) {
        return loadUsers().stream()
                .filter(user -> user.email().equalsIgnoreCase(email))
                .findFirst();
    }

    public Optional<UserAccount> findById(String userId) {
        return loadUsers().stream()
                .filter(user -> user.userId().equals(userId))
                .findFirst();
    }

    private Path usersPath() {
        return pathResolver.qapilotDir().resolve("users.json");
    }
}
