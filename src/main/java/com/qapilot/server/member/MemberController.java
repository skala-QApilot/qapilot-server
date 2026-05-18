package com.qapilot.server.member;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.member.dto.InviteMemberRequest;
import com.qapilot.server.member.dto.MemberResponse;
import com.qapilot.server.member.dto.UpdateMemberRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 멤버 관리 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@PathVariable String serviceId) {
        List<MemberResponse> members = memberService.list(serviceId);
        return ApiResponse.ok(Map.of("members", members, "count", members.size()));
    }

    @PostMapping("/invitations")
    public ResponseEntity<ApiResponse<Map<String, MemberResponse>>> invite(
            @PathVariable String serviceId,
            @RequestBody InviteMemberRequest request
    ) {
        MemberResponse member = memberService.invite(serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("member", member)));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(@PathVariable String serviceId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"members.csv\"")
                .body(memberService.exportCsv(serviceId));
    }

    @PatchMapping("/{memberId}")
    public ApiResponse<Map<String, MemberResponse>> update(
            @PathVariable String serviceId,
            @PathVariable String memberId,
            @RequestBody UpdateMemberRequest request
    ) {
        return ApiResponse.ok(Map.of("member", memberService.update(serviceId, memberId, request)));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> delete(@PathVariable String serviceId, @PathVariable String memberId) {
        memberService.delete(serviceId, memberId);
        return ResponseEntity.noContent().build();
    }
}
