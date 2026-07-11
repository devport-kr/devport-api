package kr.devport.api.domain.common.webhook;

import kr.devport.api.domain.common.cache.CacheScope;
import kr.devport.api.domain.common.webhook.dto.CrawlerJobCompletedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrawlerWebhookControllerTest {

    @Mock
    private CrawlerWebhookService crawlerWebhookService;

    @InjectMocks
    private CrawlerWebhookController crawlerWebhookController;

    @Test
    @DisplayName("job-completed webhook delegates to cache invalidation service")
    void handleJobCompleted_delegatesToCacheInvalidation() {
        CrawlerJobCompletedRequest request = new CrawlerJobCompletedRequest(
                "job-1",
                CacheScope.GIT_REPO,
                "2026-02-16T00:00:00Z",
                List.of(Map.of("projectExternalId", "github:owner/repo")),
                "sig");

        when(crawlerWebhookService.validateSignature("raw", "sig")).thenReturn(true);

        ResponseEntity<?> response = crawlerWebhookController.handleJobCompleted(request, "raw");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(crawlerWebhookService).handleJobCompleted(request);
    }

    @Test
    @DisplayName("job-completed webhook rejects invalid signature")
    void handleJobCompleted_rejectsInvalidSignature() {
        CrawlerJobCompletedRequest request = new CrawlerJobCompletedRequest(
                "job-2", CacheScope.GIT_REPO, null, null, "bad");

        when(crawlerWebhookService.validateSignature("raw", "bad")).thenReturn(false);

        ResponseEntity<?> response = crawlerWebhookController.handleJobCompleted(request, "raw");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(crawlerWebhookService, never()).handleJobCompleted(request);
    }
}
