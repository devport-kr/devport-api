package kr.devport.api.domain.newsletter.service;

import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.article.enums.ItemType;
import kr.devport.api.domain.article.repository.ArticleRepository;
import kr.devport.api.domain.common.mail.MailTemplateRenderer;
import kr.devport.api.domain.newsletter.entity.NewsletterIssue;
import kr.devport.api.domain.newsletter.enums.NewsletterIssueStatus;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import kr.devport.api.domain.newsletter.event.NewsletterIssueCreatedEvent;
import kr.devport.api.domain.newsletter.repository.NewsletterIssueRepository;
import kr.devport.api.domain.newsletter.repository.NewsletterSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeeklyDigestServiceTest {

    /** 2026-10-05(월) 08:00 KST 발송 → 9/28 08:00 ~ 10/5 08:00 KST */
    private static final ZonedDateTime SEND_TIME = ZonedDateTime.of(2026, 10, 5, 8, 0, 0, 0, WeeklyDigestWindow.KST);

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private NewsletterIssueRepository issueRepository;

    @Mock
    private NewsletterSubscriptionRepository subscriptionRepository;

    @Mock
    private NewsletterMailService mailService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private WeeklyDigestService service;

    @BeforeEach
    void setUp() {
        service = new WeeklyDigestService(articleRepository, issueRepository, subscriptionRepository, mailService,
            new MailTemplateRenderer(), eventPublisher);
        ReflectionTestUtils.setField(service, "siteUrl", "https://devport.kr");
        when(subscriptionRepository.countByStatus(NewsletterSubscriptionStatus.ACTIVE)).thenReturn(42L);
        when(issueRepository.saveAndFlush(any(NewsletterIssue.class))).thenAnswer(invocation -> {
            NewsletterIssue issue = invocation.getArgument(0);
            issue.setId(7L);
            return issue;
        });
    }

    private static Article article(long id, ItemType type, int upvotes, int comments, String title) {
        Article article = WeeklyDigestPickerTest.article(id, type, upvotes, comments);
        article.setExternalId("ext-" + id);
        article.setSummaryKoTitle(title);
        article.setSummaryKoBody("## 개요\n\n**" + title + "** 요약 본문입니다.");
        article.setUrl("https://example.com/" + id);
        return article;
    }

    private void givenCandidates(Article... articles) {
        when(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime.of(2026, 9, 27, 23, 0), LocalDateTime.of(2026, 10, 4, 23, 0)))
            .thenReturn(List.of(articles));
    }

    @Test
    void createsIssueForLastWeekAndDispatchesIt() {
        givenCandidates(
            article(1, ItemType.DISCUSSION, 900, 300, "첫 번째 <글>"),
            article(2, ItemType.DISCUSSION, 700, 10, "두 번째 글"),
            article(3, ItemType.DISCUSSION, 100, 0, "세 번째 글"),
            article(4, ItemType.BLOG, 40, 2, "블로그 글"));

        Optional<NewsletterIssue> created = service.createWeeklyIssue(SEND_TIME);

        assertThat(created).isPresent();
        NewsletterIssue issue = created.get();
        assertThat(issue.getDigestWeek()).isEqualTo("2026-W40");
        assertThat(issue.getStatus()).isEqualTo(NewsletterIssueStatus.SENDING);
        assertThat(issue.getRecipientCount()).isEqualTo(42);
        assertThat(issue.getSubject()).isEqualTo("[devport] 지난주 개발 트렌드 TOP 3 (9/28~10/4)");

        assertThat(issue.getContent())
            .contains("1. 첫 번째 <글>\nHacker News · 추천 900 · 댓글 300\n첫 번째 <글> 요약 본문입니다.\ndevport에서 읽기: https://devport.kr/articles/ext-1\n원문: https://example.com/1\n\n2. 블로그 글")
            .contains("2. 블로그 글\nDEV · 추천 40 · 댓글 2")
            .contains("3. 두 번째 글")
            .contains("devport에서 읽기: https://devport.kr/articles/ext-1")
            .contains("원문: https://example.com/1")
            .doesNotContain("세 번째 글");
        assertThat(issue.getContentHtml())
            .contains("첫 번째 &lt;글&gt;")
            .doesNotContain("<글>")
            .contains("href=\"https://devport.kr/articles/ext-4\"")
            .doesNotContain("{{");

        ArgumentCaptor<NewsletterIssueCreatedEvent> event = ArgumentCaptor.forClass(NewsletterIssueCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().issueId()).isEqualTo(7L);
    }

    @Test
    void skipsWeekThatWasAlreadySent() {
        when(issueRepository.existsByDigestWeek("2026-W40")).thenReturn(true);

        assertThat(service.createWeeklyIssue(SEND_TIME)).isEmpty();
        verify(issueRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void skipsWeekWithoutArticles() {
        givenCandidates();

        assertThat(service.createWeeklyIssue(SEND_TIME)).isEmpty();
        verify(issueRepository, never()).saveAndFlush(any());
    }

    @Test
    void subjectCountsFewerArticlesHonestly() {
        givenCandidates(article(1, ItemType.BLOG, 30, 0, "유일한 글"));

        NewsletterIssue issue = service.createWeeklyIssue(SEND_TIME).orElseThrow();

        assertThat(issue.getSubject()).isEqualTo("[devport] 지난주 개발 트렌드 TOP 1 (9/28~10/4)");
    }

    @Test
    void testSendUsesTheWindowBeingFilled() {
        // 수요일에 보내면 10/5 08:00 ~ 10/12 08:00 기간에서 고른다
        ZonedDateTime wednesday = ZonedDateTime.of(2026, 10, 7, 15, 0, 0, 0, WeeklyDigestWindow.KST);
        when(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime.of(2026, 10, 4, 23, 0), LocalDateTime.of(2026, 10, 11, 23, 0)))
            .thenReturn(List.of(article(1, ItemType.BLOG, 30, 0, "이번 주 글")));

        assertThat(service.sendTest("admin@devport.kr", wednesday)).isTrue();
        verify(mailService).sendTestIssue(any(), anyString(), anyString(), anyString());

        assertThat(service.sendTest("admin@devport.kr", SEND_TIME.plusWeeks(2))).isFalse();
    }

    @Test
    void excerptStripsMarkdownAndTruncates() {
        String markdown = """
            ## 제목

            ![배너](https://img.example/a.png)
            **굵게** 쓴 [링크 텍스트](https://x.dev)와 `코드`입니다.

            ```java
            System.out.println("hidden");
            ```
            - 목록 항목
            """;

        assertThat(WeeklyDigestService.excerpt(markdown, 200))
            .isEqualTo("굵게 쓴 링크 텍스트와 코드입니다. 목록 항목");
        assertThat(WeeklyDigestService.excerpt("가나다라마바사", 3)).isEqualTo("가나다…");
        assertThat(WeeklyDigestService.excerpt(null, 10)).isEmpty();
    }
}
