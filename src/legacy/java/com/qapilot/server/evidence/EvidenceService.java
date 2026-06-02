package com.qapilot.server.evidence;

import com.qapilot.server.evidence.store.EvidenceFileStore;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 증적 조회 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class EvidenceService {

    private final ServiceDomainService serviceDomainService;
    private final EvidenceFileStore evidenceFileStore;

    public EvidenceService(ServiceDomainService serviceDomainService, EvidenceFileStore evidenceFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.evidenceFileStore = evidenceFileStore;
    }

    public Map<String, Object> index(String serviceId, String traceId) {
        return evidenceFileStore.index(qapilotDir(serviceId), traceId);
    }

    public Map<String, Object> tcEvidence(String serviceId, String traceId, String tcId) {
        return evidenceFileStore.tcEvidence(qapilotDir(serviceId), traceId, tcId);
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }
}
