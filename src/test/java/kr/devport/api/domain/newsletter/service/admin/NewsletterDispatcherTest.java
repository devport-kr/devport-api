package kr.devport.api.domain.newsletter.service.admin;

import kr.devport.api.domain.newsletter.entity.NewsletterIssue;
import kr.devport.api.domain.newsletter.entity.NewsletterSubscription;
import kr.devport.api.domain.newsletter.enums.NewsletterIssueStatus;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import kr.devport.api.domain.newsletter.repository.NewsletterIssueRepository;
import kr.devport.api.domain.newsletter.repository.NewsletterSubscriptionRepository;
import kr.devport.api.domain.newsletter.service.NewsletterMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewsletterDispatcherTest {

    @Mock
    private NewsletterIssueRepository issueRepository;

    @Mock
    private NewsletterSubscriptionRepository subscriptionRepository;

    @Mock
    private NewsletterMailService mailService;

    private NewsletterDispatcher dispatcher;
    private NewsletterIssue issue;

    @BeforeEach
    void setUp() {
        dispatcher = new NewsletterDispatcher(issueRepository, subscriptionRepository, mailService);
        ReflectionTestUtils.setField(dispatcher, "batchSize", 2);
        ReflectionTestUtils.setField(dispatcher, "batchIntervalMs", 0L);

        issue = NewsletterIssue.builder()
            .id(1L)
            .subject("제목")
            .content("본문")
            .status(NewsletterIssueStatus.SENDING)
            .recipientCount(3)
            .createdAt(LocalDateTime.now())
            .build();
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(NewsletterIssue.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private NewsletterSubscription active(long id) {
        return NewsletterSubscription.builder()
            .id(id)
            .email("user" + id + "@example.com")
            .status(NewsletterSubscriptionStatus.ACTIVE)
            .unsubscribeToken("unsub-" + id)
            .build();
    }

    @Test
    void sendsToAllActiveSubscribersInKeysetBatches() {
        List<NewsletterSubscription> first = List.of(active(1), active(2));
        List<NewsletterSubscription> second = List.of(active(5));
        when(subscriptionRepository.findByStatusAndIdGreaterThanOrderByIdAsc(
            NewsletterSubscriptionStatus.ACTIVE, 0L, Limit.of(2))).thenReturn(first);
        when(subscriptionRepository.findByStatusAndIdGreaterThanOrderByIdAsc(
            NewsletterSubscriptionStatus.ACTIVE, 2L, Limit.of(2))).thenReturn(second);
        when(mailService.sendIssueBatch("제목", "본문", first)).thenReturn(0);
        when(mailService.sendIssueBatch("제목", "본문", second)).thenReturn(1);

        dispatcher.dispatch(1L);

        assertThat(issue.getSentCount()).isEqualTo(2);
        assertThat(issue.getFailedCount()).isEqualTo(1);
        assertThat(issue.getRecipientCount()).isEqualTo(3);
        assertThat(issue.getStatus()).isEqualTo(NewsletterIssueStatus.PARTIALLY_FAILED);
        assertThat(issue.getCompletedAt()).isNotNull();
    }

    @Test
    void marksSentWhenNoSubscribers() {
        when(subscriptionRepository.findByStatusAndIdGreaterThanOrderByIdAsc(any(), any(), any())).thenReturn(List.of());

        dispatcher.dispatch(1L);

        assertThat(issue.getStatus()).isEqualTo(NewsletterIssueStatus.SENT);
        assertThat(issue.getRecipientCount()).isZero();
        verify(mailService, never()).sendIssueBatch(any(), any(), anyList());
    }

    @Test
    void marksFailedWhenDispatchAbortsBeforeAnythingIsSent() {
        when(subscriptionRepository.findByStatusAndIdGreaterThanOrderByIdAsc(any(), eq(0L), any()))
            .thenThrow(new IllegalStateException("db down"));

        dispatcher.dispatch(1L);

        assertThat(issue.getStatus()).isEqualTo(NewsletterIssueStatus.FAILED);
        assertThat(issue.getRecipientCount()).isEqualTo(3);
    }

    @Test
    void skipsIssueThatIsNoLongerSending() {
        issue.setStatus(NewsletterIssueStatus.SENT);

        dispatcher.dispatch(1L);

        verify(subscriptionRepository, never()).findByStatusAndIdGreaterThanOrderByIdAsc(any(), any(), any());
    }
}
