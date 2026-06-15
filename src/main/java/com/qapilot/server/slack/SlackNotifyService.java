package com.qapilot.server.slack;

import com.qapilot.server.slack.dto.SlackNotifyResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Slack DM 파일 전송 — 수신자 이메일별로 lookup → DM open → 3-step 업로드.
 *
 * <p>Author: C
 * <br>Created: 2026-06-15
 */
@Service
public class SlackNotifyService {

    private static final Logger log = LoggerFactory.getLogger(SlackNotifyService.class);

    private final SlackClient slackClient;

    public SlackNotifyService(SlackClient slackClient) {
        this.slackClient = slackClient;
    }

    public List<SlackNotifyResult> sendFile(
            List<String> recipients,
            byte[] fileBytes,
            String filename,
            String message
    ) {
        List<SlackNotifyResult> results = new ArrayList<>();
        for (String email : recipients) {
            results.add(sendToRecipient(email, fileBytes, filename, message));
        }
        return results;
    }

    private SlackNotifyResult sendToRecipient(String email, byte[] fileBytes, String filename, String message) {
        try {
            Optional<String> slackUserId = slackClient.lookupUserByEmail(email);
            if (slackUserId.isEmpty()) {
                return new SlackNotifyResult(email, "not_found");
            }
            String channelId = slackClient.openDm(slackUserId.get());
            Map<String, Object> uploadInfo = slackClient.getUploadUrlExternal(filename, fileBytes.length);
            String uploadUrl = String.valueOf(uploadInfo.get("upload_url"));
            String fileId = String.valueOf(uploadInfo.get("file_id"));
            slackClient.uploadFileBytes(uploadUrl, fileBytes, filename);
            slackClient.completeUploadExternal(fileId, filename, channelId, message);
            return new SlackNotifyResult(email, "sent");
        } catch (Exception e) {
            log.warn("Slack 파일 전송 실패: email={}", email, e);
            return new SlackNotifyResult(email, "failed");
        }
    }
}
