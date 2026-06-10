package com.qapilot.server.result;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.result.dto.ResultResponse;
import com.qapilot.server.result.dto.ResultStatisticsResponse;
import com.qapilot.server.run.RunReader;
import com.qapilot.server.run.persistence.ActionMappingEntity;
import com.qapilot.server.run.persistence.ActionMappingRepository;
import com.qapilot.server.run.persistence.TcArtifactEntity;
import com.qapilot.server.run.persistence.TcArtifactRepository;
import com.qapilot.server.run.persistence.TcResultEntity;
import com.qapilot.server.run.persistence.TcResultRepository;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.storage.S3Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * trace 기반 결과 조회 유스케이스.
 *
 * <p>PR-15c — file → DB read 전환. ResultResponse.fromTrace 가 보는 Map 구조는
 * RunReader 가 그대로 재현 (tc_results 는 tc_results 테이블에서 ui kind 만 추출).
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class ResultQueryService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ServiceDomainService serviceDomainService;
    private final RunReader runReader;
    private final TcResultRepository tcResultRepository;
    private final TcArtifactRepository tcArtifactRepository;
    private final ActionMappingRepository actionMappingRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    public ResultQueryService(
            ServiceDomainService serviceDomainService,
            RunReader runReader,
            TcResultRepository tcResultRepository,
            TcArtifactRepository tcArtifactRepository,
            ActionMappingRepository actionMappingRepository,
            S3Service s3Service,
            ObjectMapper objectMapper
    ) {
        this.serviceDomainService = serviceDomainService;
        this.runReader = runReader;
        this.tcResultRepository = tcResultRepository;
        this.tcArtifactRepository = tcArtifactRepository;
        this.actionMappingRepository = actionMappingRepository;
        this.s3Service = s3Service;
        this.objectMapper = objectMapper;
    }

    /**
     * TC 결과 본문 — tc_results.payload (JSONB) decode 해서 Map 으로 반환.
     * kind = ui / api / db. 해당 결과가 없으면 빈 Map (호출자 graceful).
     */
    public Map<String, Object> getTcPayload(String serviceId, String runId, String tsId, String tcId, String kind) {
        serviceDomainService.getById(serviceId);  // 권한 검증
        return tcResultRepository
                .findFirstByRunIdAndTsIdAndTcIdAndKind(UUID.fromString(runId), tsId, tcId, kind)
                .map(e -> decodeJson(e.getPayload()))
                .orElse(Map.of());
    }

    /** TC 의 ActionMapping (steps + api_endpoint) — action_mappings 테이블 최신 version. 없으면 빈 Map. */
    public Map<String, Object> getActionMapping(String serviceId, String tcId) {
        serviceDomainService.getById(serviceId);  // 권한 검증
        return actionMappingRepository
                .findFirstByServiceIdAndTcIdOrderByVersionDesc(UUID.fromString(serviceId), tcId)
                .map(ActionMappingEntity::getPayload)
                .map(this::decodeJson)
                .orElse(Map.of());
    }

    private Map<String, Object> decodeJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** run 의 모든 TC 결과 목록 (kind/status) — PASS/FAIL 탭 리스트 채우기용. */
    public List<Map<String, Object>> listTcResults(String serviceId, String runId) {
        serviceDomainService.getById(serviceId);  // 권한 검증
        return tcResultRepository.findAllByRunId(UUID.fromString(runId)).stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("ts_id", e.getTsId());
                    m.put("tc_id", e.getTcId());
                    m.put("kind", e.getKind());
                    m.put("status", toLegacyStatus(e.getStatus()));
                    return m;
                })
                .toList();
    }

    /** DB 상태 어휘(pass/fail/skip) → 응답 어휘(passed/failed/skipped). RunReader 와 동일 규칙. */
    private String toLegacyStatus(String dbStatus) {
        if (dbStatus == null) {
            return null;
        }
        return switch (dbStatus) {
            case "pass" -> "passed";
            case "fail" -> "failed";
            case "skip" -> "skipped";
            default -> dbStatus;
        };
    }

    /** 에러 시점 스크린샷 — S3 에서 byte[] 로 가져옴. step 미지정 시 step=1 (fail step). */
    public byte[] getScreenshot(String serviceId, String runId, String tsId, String tcId, int step) {
        serviceDomainService.getById(serviceId);
        TcResultEntity tr = tcResultRepository
                .findFirstByRunIdAndTsIdAndTcIdAndKind(UUID.fromString(runId), tsId, tcId, "ui")
                .orElseThrow(() -> new QapilotException(ErrorCode.RESULT_001));
        TcArtifactEntity art = tcArtifactRepository
                .findFirstByTcResultIdAndStepIndexAndKind(tr.getId(), step, "png")
                .orElseThrow(() -> new QapilotException(ErrorCode.RESULT_001));
        byte[] data = s3Service.get(art.getS3Key());
        if (data == null) {
            throw new QapilotException(ErrorCode.RESULT_001, "S3 에서 스크린샷을 가져오지 못했습니다.");
        }
        return data;
    }

    public List<ResultResponse> list(String serviceId, String status, int limit, int offset) {
        return allResults(serviceId).stream()
                .filter(result -> status == null || status.isBlank() || status.equals(result.status()))
                .skip(Math.max(offset, 0))
                .limit(Math.max(limit, 1))
                .toList();
    }

    public int total(String serviceId, String status) {
        return (int) allResults(serviceId).stream()
                .filter(result -> status == null || status.isBlank() || status.equals(result.status()))
                .count();
    }

    public ResultResponse get(String serviceId, String traceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        Map<String, Object> trace = runReader.findById(traceId)
                .orElseThrow(() -> new QapilotException(ErrorCode.RESULT_001));
        if (!"test".equals(trace.get("command"))) {
            throw new QapilotException(ErrorCode.RESULT_001);
        }
        // service 존재만 확인하고 별 사용 없음 — 호환 유지를 위해 호출
        service.serviceId();
        return ResultResponse.fromTrace(trace);
    }

    public ResultStatisticsResponse statistics(String serviceId) {
        List<ResultResponse> results = allResults(serviceId);
        int passed = (int) results.stream().filter(result -> "completed".equals(result.status())).count();
        // "aborted": 파이프라인이 비정상 종료(예외/Ctrl+C 등). TC-level "failed" 와 다른 축.
        int failed = (int) results.stream().filter(result -> "aborted".equals(result.status())).count();
        Double passRate = results.isEmpty() ? null : (passed * 100.0) / results.size();
        return new ResultStatisticsResponse(results.size(), passed, failed, passRate);
    }

    public List<ResultResponse> allResults(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return runReader.listByServiceId(service.serviceId()).stream()
                .filter(trace -> "test".equals(trace.get("command")))
                .map(ResultResponse::fromTrace)
                .toList();
    }
}
