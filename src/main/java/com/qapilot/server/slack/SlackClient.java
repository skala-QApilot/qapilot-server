package com.qapilot.server.slack;

import com.qapilot.server.common.config.QapilotProperties;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Slack Web API 연동 클라이언트 — 이메일로 사용자 조회 → DM 채널 open → 파일 업로드(3-step).
 *
 * <p>Author: C
 * <br>Created: 2026-06-15
 */
@Component
public class SlackClient {

    private final WebClient webClient;
    private final QapilotProperties properties;

    public SlackClient(WebClient.Builder builder, QapilotProperties properties) {
        this.properties = properties;
        this.webClient = builder.baseUrl("https://slack.com/api").build();
    }

    /** 이메일로 Slack 사용자 ID 조회. 워크스페이스에 없으면 empty. */
    public Optional<String> lookupUserByEmail(String email) {
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/users.lookupByEmail").queryParam("email", email).build())
                .headers(this::authHeaders)
                .retrieve()
                .bodyToMono(mapType())
                .block();
        if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
            return Optional.empty();
        }
        Object user = response.get("user");
        if (user instanceof Map<?, ?> userMap) {
            return Optional.ofNullable((String) userMap.get("id"));
        }
        return Optional.empty();
    }

    /** 1:1 DM 채널을 열고 채널 ID 를 반환한다. */
    public String openDm(String slackUserId) {
        Map<String, Object> response = webClient.post()
                .uri("/conversations.open")
                .headers(this::authHeaders)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("users", slackUserId))
                .retrieve()
                .bodyToMono(mapType())
                .block();
        if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
            throw new QapilotException(ErrorCode.SLACK_001, "Slack DM 채널을 열 수 없습니다: " + errorOf(response));
        }
        Object channel = response.get("channel");
        if (channel instanceof Map<?, ?> channelMap) {
            return (String) channelMap.get("id");
        }
        throw new QapilotException(ErrorCode.SLACK_001, "Slack DM 채널 응답이 올바르지 않습니다.");
    }

    /** 업로드용 URL + file_id 발급. */
    public Map<String, Object> getUploadUrlExternal(String filename, long length) {
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/files.getUploadURLExternal")
                        .queryParam("filename", filename)
                        .queryParam("length", length)
                        .build())
                .headers(this::authHeaders)
                .retrieve()
                .bodyToMono(mapType())
                .block();
        if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
            throw new QapilotException(ErrorCode.SLACK_001, "Slack 업로드 URL 발급에 실패했습니다: " + errorOf(response));
        }
        return response;
    }

    /** 발급받은 업로드 URL에 파일 바이트를 전송한다 (인증 헤더 불필요). */
    public void uploadFileBytes(String uploadUrl, byte[] bytes, String filename) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        webClient.post()
                .uri(uploadUrl)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    /** 업로드 완료 처리 — DM 채널에 파일을 공유하고 initial_comment 를 남긴다. */
    public void completeUploadExternal(String fileId, String filename, String channelId, String initialComment) {
        Map<String, Object> body = new HashMap<>();
        body.put("files", List.of(Map.of("id", fileId, "title", filename)));
        body.put("channel_id", channelId);
        if (initialComment != null && !initialComment.isBlank()) {
            body.put("initial_comment", initialComment);
        }
        Map<String, Object> response = webClient.post()
                .uri("/files.completeUploadExternal")
                .headers(this::authHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(mapType())
                .block();
        if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
            throw new QapilotException(ErrorCode.SLACK_001, "Slack 파일 업로드 완료 처리에 실패했습니다: " + errorOf(response));
        }
    }

    private void authHeaders(HttpHeaders headers) {
        if (!properties.slack().hasBotToken()) {
            throw new QapilotException(ErrorCode.SLACK_001);
        }
        headers.setBearerAuth(properties.slack().botToken());
    }

    private static String errorOf(Map<String, Object> response) {
        return response == null ? "no response" : String.valueOf(response.get("error"));
    }

    @SuppressWarnings("unchecked")
    private Class<Map<String, Object>> mapType() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }
}
