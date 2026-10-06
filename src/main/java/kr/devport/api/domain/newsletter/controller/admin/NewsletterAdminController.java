package kr.devport.api.domain.newsletter.controller.admin;

import jakarta.validation.Valid;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.newsletter.dto.request.admin.NewsletterIssueRequest;
import kr.devport.api.domain.newsletter.dto.request.admin.NewsletterTestSendRequest;
import kr.devport.api.domain.newsletter.dto.request.admin.WeeklyDigestTestSendRequest;
import kr.devport.api.domain.newsletter.dto.response.admin.NewsletterIssueResponse;
import kr.devport.api.domain.newsletter.dto.response.admin.NewsletterStatsResponse;
import kr.devport.api.domain.newsletter.dto.response.admin.WeeklyDigestPreviewResponse;
import kr.devport.api.domain.newsletter.service.WeeklyDigestService;
import kr.devport.api.domain.newsletter.service.admin.NewsletterAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/newsletter")
@RequiredArgsConstructor
public class NewsletterAdminController {

    private final NewsletterAdminService newsletterAdminService;
    private final WeeklyDigestService weeklyDigestService;

    @GetMapping("/stats")
    public ResponseEntity<NewsletterStatsResponse> getStats() {
        return ResponseEntity.ok(newsletterAdminService.getStats());
    }

    @GetMapping("/issues")
    public ResponseEntity<Page<NewsletterIssueResponse>> getIssues(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(newsletterAdminService.getIssues(page, size));
    }

    /** 단일 주소로 미리보기 발송 (동기) */
    @PostMapping("/issues/test")
    public ResponseEntity<Map<String, String>> sendTest(@Valid @RequestBody NewsletterTestSendRequest request) {
        newsletterAdminService.sendTest(request);
        return ResponseEntity.ok(Map.of("message", "테스트 메일을 발송했습니다."));
    }

    /** ACTIVE 구독자 전체에게 발송 (비동기). 진행 상황은 GET /issues 로 확인한다. */
    @PostMapping("/issues")
    public ResponseEntity<NewsletterIssueResponse> createIssue(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody NewsletterIssueRequest request
    ) {
        NewsletterIssueResponse response = newsletterAdminService.createIssue(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /** 다음 월요일에 발송될 주간 다이제스트의 지금까지 선정 결과 */
    @GetMapping("/weekly-digest/preview")
    public ResponseEntity<WeeklyDigestPreviewResponse> previewWeeklyDigest() {
        return ResponseEntity.ok(weeklyDigestService.preview(ZonedDateTime.now()));
    }

    /** 다음 주간 다이제스트를 지금까지의 글로 만들어 단일 주소로 발송 (동기) */
    @PostMapping("/weekly-digest/test")
    public ResponseEntity<Map<String, String>> sendWeeklyDigestTest(@Valid @RequestBody WeeklyDigestTestSendRequest request) {
        if (!weeklyDigestService.sendTest(request.getEmail().trim(), ZonedDateTime.now())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "아직 선정할 글이 없습니다."));
        }
        return ResponseEntity.ok(Map.of("message", "테스트 메일을 발송했습니다."));
    }
}
