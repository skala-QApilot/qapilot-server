package com.qapilot.server.file.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
public interface DomainDocumentRepository extends JpaRepository<DomainDocumentEntity, UUID> {

    List<DomainDocumentEntity> findAllByServiceIdOrderByUploadedAtDesc(UUID serviceId);

    List<DomainDocumentEntity> findAllByFileIdOrderByVersionDesc(UUID fileId);

    Optional<DomainDocumentEntity> findFirstByFileIdOrderByVersionDesc(UUID fileId);
}
