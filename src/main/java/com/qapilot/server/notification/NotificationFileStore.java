package com.qapilot.server.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.notification.domain.Notification;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 서비스 범위 notifications.json 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class NotificationFileStore {

    private final JsonFileStore jsonFileStore;

    public NotificationFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<Notification> load(Path qapilotDir) {
        return new ArrayList<>(jsonFileStore.readOrDefault(notificationsPath(qapilotDir), new TypeReference<>() {
        }, List.of()));
    }

    public void save(Path qapilotDir, List<Notification> notifications) {
        jsonFileStore.write(notificationsPath(qapilotDir), notifications);
    }

    public Optional<Notification> findById(Path qapilotDir, String notificationId) {
        return load(qapilotDir).stream()
                .filter(n -> notificationId.equals(n.notificationId()))
                .findFirst();
    }

    private Path notificationsPath(Path qapilotDir) {
        return qapilotDir.resolve("notifications.json");
    }
}
