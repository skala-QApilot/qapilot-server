package com.qapilot.server.service.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.common.files.QapilotPathResolver;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * services.json 파일 기반 서비스 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class ServiceFileStore {

    private final JsonFileStore jsonFileStore;
    private final QapilotPathResolver pathResolver;

    public ServiceFileStore(JsonFileStore jsonFileStore, QapilotPathResolver pathResolver) {
        this.jsonFileStore = jsonFileStore;
        this.pathResolver = pathResolver;
    }

    public List<QapilotService> loadServices() {
        return new ArrayList<>(jsonFileStore.readOrDefault(servicesPath(), new TypeReference<>() {
        }, List.of()));
    }

    public void saveServices(List<QapilotService> services) {
        jsonFileStore.write(servicesPath(), services);
    }

    public Optional<QapilotService> findById(String serviceId) {
        return loadServices().stream()
                .filter(service -> service.serviceId().equals(serviceId))
                .findFirst();
    }

    public Optional<QapilotService> findByProjectSlug(String projectSlug) {
        return loadServices().stream()
                .filter(service -> service.projectSlug().equals(projectSlug))
                .findFirst();
    }

    public Optional<QapilotService> findByServerAuthToken(String token) {
        return loadServices().stream()
                .filter(service -> service.serverAuthToken().equals(token))
                .findFirst();
    }

    private Path servicesPath() {
        return pathResolver.qapilotDir().resolve("services.json");
    }
}
