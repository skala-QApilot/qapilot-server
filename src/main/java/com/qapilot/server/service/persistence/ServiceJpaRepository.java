package com.qapilot.server.service.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
public interface ServiceJpaRepository extends JpaRepository<ServiceEntity, UUID> {
}
