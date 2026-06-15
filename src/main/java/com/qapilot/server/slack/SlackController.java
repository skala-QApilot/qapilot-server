package com.qapilot.server.slack;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.slack.dto.SlackNotifyResult;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 테스트 결과 파일(CSV/PDF)을 Slack DM 으로 공유하는 API.
 *
 * <p>Author: C
 * <br>Created: 2026-06-15
 */
@RestController
@RequestMapping("/api/services/{serviceId}/results")
public class SlackController {

    private final SlackNotifyService slackNotifyService;
    private final ServiceDomainService serviceDomainService;

    public SlackController(SlackNotifyService slackNotifyService, ServiceDomainService serviceDomainService) {
        this.slackNotifyService = slackNotifyService;
        this.serviceDomainService = serviceDomainService;
    }

    @PostMapping("/{traceId}/slack-notify")
    public ApiResponse<Map<String, Object>> notify(
            @PathVariable String serviceId,
            @PathVariable String traceId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("recipients") List<String> recipients,
            @RequestParam(value = "message", required = false) String message
    ) {
        serviceDomainService.getById(serviceId);
        List<SlackNotifyResult> results = slackNotifyService.sendFile(recipients, bytes(file), file.getOriginalFilename(), message);
        return ApiResponse.ok(Map.of("results", results));
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "업로드 파일을 읽을 수 없습니다.");
        }
    }
}
