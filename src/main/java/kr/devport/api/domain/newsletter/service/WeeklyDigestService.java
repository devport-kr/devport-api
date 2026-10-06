package kr.devport.api.domain.newsletter.service;

import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.article.enums.ItemType;
import kr.devport.api.domain.article.repository.ArticleRepository;
import kr.devport.api.domain.common.mail.MailTemplateRenderer;
import kr.devport.api.domain.newsletter.dto.response.admin.WeeklyDigestArticleResponse;
import kr.devport.api.domain.newsletter.dto.response.admin.WeeklyDigestPreviewResponse;
import kr.devport.api.domain.newsletter.entity.NewsletterIssue;
import kr.devport.api.domain.newsletter.enums.NewsletterIssueStatus;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import kr.devport.api.domain.newsletter.event.NewsletterIssueCreatedEvent;
import kr.devport.api.domain.newsletter.repository.NewsletterIssueRepository;
import kr.devport.api.domain.newsletter.repository.NewsletterSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 지난주 가장 관심을 받은 글 {@value #ARTICLE_COUNT}개를 골라 ACTIVE 구독자 전체에게 보내는 주간 다이제스트.
 * 발송은 관리자 뉴스레터와 같은 경로(NewsletterIssue + {@link NewsletterIssueCreatedEvent})를 탄다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyDigestService {

    static final int ARTICLE_COUNT = 3;
    private static final int MAX_PER_ITEM_TYPE = 2;
    private static final int EXCERPT_LENGTH = 140;

    private static final String TEMPLATE = "newsletter/weekly-digest";
    private static final String ITEM_TEMPLATE = "newsletter/weekly-digest-item";

    private final ArticleRepository articleRepository;
    private final NewsletterIssueRepository issueRepository;
    private final NewsletterSubscriptionRepository subscriptionRepository;
    private final NewsletterMailService mailService;
    private final MailTemplateRenderer templates;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.newsletter.site-url}")
    private String siteUrl;

    record Digest(WeeklyDigestWindow window, int candidateCount, List<Article> articles, String subject, String text, String html) {
    }

    /**
     * 방금 끝난 주의 다이제스트를 만들고, 커밋 후 비동기로 발송한다.
     * 이미 만든 주이거나 고를 글이 없으면 만들지 않는다.
     * 여러 인스턴스가 동시에 실행하면(blue/green 전환 중) digest_week unique 제약으로 하나만 성공한다.
     */
    @Transactional
    public Optional<NewsletterIssue> createWeeklyIssue(ZonedDateTime now) {
        WeeklyDigestWindow window = WeeklyDigestWindow.endingAtOrBefore(now);
        if (issueRepository.existsByDigestWeek(window.weekKey())) {
            log.info("weekly-digest: {} already created, skipping", window.weekKey());
            return Optional.empty();
        }

        Digest digest = build(window);
        if (digest.articles().isEmpty()) {
            log.warn("weekly-digest: no articles for {} ({} candidates), skipping", window.weekKey(), digest.candidateCount());
            return Optional.empty();
        }

        NewsletterIssue issue = issueRepository.saveAndFlush(NewsletterIssue.builder()
            .subject(digest.subject())
            .content(digest.text())
            .contentHtml(digest.html())
            .digestWeek(window.weekKey())
            .status(NewsletterIssueStatus.SENDING)
            .recipientCount((int) subscriptionRepository.countByStatus(NewsletterSubscriptionStatus.ACTIVE))
            .createdAt(LocalDateTime.now())
            .build());

        eventPublisher.publishEvent(new NewsletterIssueCreatedEvent(issue.getId()));
        log.info("weekly-digest: issue created, issueId={}, week={}, articles={}, candidates={}, recipients={}",
            issue.getId(), window.weekKey(), digest.articles().stream().map(Article::getId).toList(),
            digest.candidateCount(), issue.getRecipientCount());
        return Optional.of(issue);
    }

    /** 다음 발송분의 지금까지 선정 결과 */
    @Transactional(readOnly = true)
    public WeeklyDigestPreviewResponse preview(ZonedDateTime now) {
        Digest digest = build(WeeklyDigestWindow.endingAtOrBefore(now).next());
        return WeeklyDigestPreviewResponse.builder()
            .digestWeek(digest.window().weekKey())
            .windowStart(digest.window().start().toLocalDateTime())
            .windowEnd(digest.window().end().toLocalDateTime())
            .subject(digest.subject())
            .candidateCount(digest.candidateCount())
            .articles(digest.articles().stream()
                .map(article -> WeeklyDigestArticleResponse.builder()
                    .externalId(article.getExternalId())
                    .title(article.getSummaryKoTitle())
                    .source(sourceLabel(article))
                    .votes(WeeklyDigestPicker.votes(article))
                    .comments(WeeklyDigestPicker.comments(article))
                    .articleUrl(articleUrl(article))
                    .originalUrl(article.getUrl())
                    .build())
                .toList())
            .build();
    }

    /**
     * 다음 발송분을 지금까지의 글로 만들어 한 주소에만 보낸다.
     *
     * @return 고를 글이 없어 보내지 않았으면 false
     */
    public boolean sendTest(String email, ZonedDateTime now) {
        Digest digest = build(WeeklyDigestWindow.endingAtOrBefore(now).next());
        if (digest.articles().isEmpty()) {
            return false;
        }
        mailService.sendTestIssue(email, digest.subject(), digest.text(), digest.html());
        return true;
    }

    Digest build(WeeklyDigestWindow window) {
        List<Article> candidates = articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            window.utcStart(), window.utcEnd());
        List<Article> picks = WeeklyDigestPicker.pick(candidates, ARTICLE_COUNT, MAX_PER_ITEM_TYPE);

        List<String> itemsHtml = new ArrayList<>(picks.size());
        List<String> itemsText = new ArrayList<>(picks.size());
        for (int i = 0; i < picks.size(); i++) {
            Article article = picks.get(i);
            Map<String, String> vars = Map.of(
                "rank", String.valueOf(i + 1),
                "title", article.getSummaryKoTitle(),
                "meta", meta(article),
                "excerpt", excerpt(article.getSummaryKoBody(), EXCERPT_LENGTH),
                "articleUrl", articleUrl(article),
                "originalUrl", article.getUrl());
            itemsHtml.add(templates.render(ITEM_TEMPLATE + ".html", vars));
            itemsText.add(templates.render(ITEM_TEMPLATE + ".txt", vars));
        }

        String heading = "지난주 개발 트렌드 TOP " + picks.size();
        String html = templates.render(TEMPLATE + ".html", Map.of(
            "heading", heading, "period", window.label(), "siteUrl", siteUrl, "items", String.join("", itemsHtml)));
        String text = templates.render(TEMPLATE + ".txt", Map.of(
            "heading", heading, "period", window.label(), "siteUrl", siteUrl, "items", String.join("\n", itemsText)));
        String subject = "[devport] " + heading + " (" + window.label() + ")";
        return new Digest(window, candidates.size(), picks, subject, text, html);
    }

    String articleUrl(Article article) {
        return siteUrl + "/articles/" + article.getExternalId();
    }

    /** 예: Hacker News · 추천 812 · 댓글 340 */
    static String meta(Article article) {
        return String.format("%s · 추천 %,d · 댓글 %,d",
            sourceLabel(article), WeeklyDigestPicker.votes(article), WeeklyDigestPicker.comments(article));
    }

    /** HN 글의 source는 원문 도메인이라 item type으로 구분한다. */
    static String sourceLabel(Article article) {
        if (article.getItemType() == ItemType.DISCUSSION) {
            return "Hacker News";
        }
        if ("devto".equals(article.getSource())) {
            return "DEV";
        }
        return article.getSource();
    }

    /** Markdown 요약 본문에서 코드/이미지/서식을 걷어내고 앞부분만 남긴다. */
    static String excerpt(String markdown, int maxLength) {
        if (markdown == null) {
            return "";
        }
        String text = markdown
            .replaceAll("(?s)```.*?```", " ")
            .replaceAll("!\\[[^]]*]\\([^)]*\\)", " ")
            .replaceAll("\\[([^]]*)]\\([^)]*\\)", "$1")
            .replaceAll("</?[a-zA-Z][^>]*>", " ")
            .replaceAll("(?m)^\\s*#{1,6}\\s.*$", " ")
            .replaceAll("(?m)^\\s*(>|[-*+]|\\d+\\.)\\s+", "")
            .replaceAll("\\*\\*|__|~~|`", "")
            .replaceAll("\\s+", " ")
            .strip();
        if (text.length() <= maxLength) {
            return text;
        }
        int end = Character.isHighSurrogate(text.charAt(maxLength - 1)) ? maxLength - 1 : maxLength;
        return text.substring(0, end).strip() + "…";
    }
}
