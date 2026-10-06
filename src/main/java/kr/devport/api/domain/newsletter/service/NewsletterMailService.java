package kr.devport.api.domain.newsletter.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import kr.devport.api.domain.common.exception.EmailDeliveryException;
import kr.devport.api.domain.common.logging.LogSanitizer;
import kr.devport.api.domain.common.mail.MailTemplateRenderer;
import kr.devport.api.domain.newsletter.entity.NewsletterSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 뉴스레터 관련 메일 발송. 본문은 classpath:templates/mail/newsletter/ 의 템플릿을 쓴다.
 * 수신 동의/거부 처리 결과 안내 메일은 「정보통신망법」 제50조에 따른 통지이며, 실패해도 요청을 막지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterMailService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"']*[^\\s<>\"'.,;:!?)\\]]");

    private static final String VERIFICATION_TEMPLATE = "newsletter/verification.txt";
    private static final String SUBSCRIBED_TEMPLATE = "newsletter/subscribed.txt";
    private static final String UNSUBSCRIBED_TEMPLATE = "newsletter/unsubscribed.txt";
    private static final String ISSUE_TEXT_TEMPLATE = "newsletter/issue.txt";
    private static final String ISSUE_HTML_TEMPLATE = "newsletter/issue.html";

    private final JavaMailSender mailSender;
    private final MailTemplateRenderer templates;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.newsletter.sender-name:devport}")
    private String senderName;

    @Value("${app.newsletter.site-url}")
    private String siteUrl;

    @Value("${app.newsletter.api-url}")
    private String apiUrl;

    public void sendVerificationEmail(String email, String rawToken) {
        String text = templates.render(VERIFICATION_TEMPLATE, Map.of(
            "confirmUrl", siteUrl + "/newsletter/confirm?token=" + rawToken,
            "senderName", senderName));

        try {
            mailSender.send(buildPlainMessage(email, "[devport] 뉴스레터 구독 이메일 인증", text));
            log.debug("Newsletter verification email sent to {}", LogSanitizer.maskEmail(email));
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new EmailDeliveryException("인증 메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    public void sendSubscribedNotice(NewsletterSubscription subscription) {
        sendNoticeQuietly(subscription.getEmail(), "[devport] 뉴스레터 구독이 완료되었습니다", SUBSCRIBED_TEMPLATE, Map.of(
            "senderName", senderName,
            "siteUrl", siteUrl,
            "consentedAt", DATE_TIME_FORMAT.format(subscription.getConsentedAt()),
            "unsubscribeUrl", unsubscribePageUrl(subscription.getUnsubscribeToken())));
    }

    public void sendUnsubscribedNotice(String email, LocalDateTime unsubscribedAt) {
        sendNoticeQuietly(email, "[devport] 뉴스레터 수신 거부가 처리되었습니다", UNSUBSCRIBED_TEMPLATE, Map.of(
            "senderName", senderName,
            "siteUrl", siteUrl,
            "unsubscribedAt", DATE_TIME_FORMAT.format(unsubscribedAt)));
    }

    /** 관리자 미리보기 발송. 수신거부 링크는 동작하지 않는 더미 토큰을 쓴다. */
    public void sendTestIssue(String email, String subject, String content) {
        try {
            mailSender.send(buildIssueMessage(email, "preview", "[테스트] " + subject, content));
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new EmailDeliveryException("테스트 메일 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 한 SMTP 연결로 묶어서 발송한다.
     *
     * @return 발송에 실패한 수신자 수
     */
    public int sendIssueBatch(String subject, String content, List<NewsletterSubscription> recipients) {
        List<MimeMessage> messages = new ArrayList<>(recipients.size());
        int failed = 0;
        for (NewsletterSubscription recipient : recipients) {
            try {
                messages.add(buildIssueMessage(recipient.getEmail(), recipient.getUnsubscribeToken(), subject, content));
            } catch (MessagingException | UnsupportedEncodingException e) {
                failed++;
                log.warn("Failed to build newsletter message for subscriptionId={}", recipient.getId(), e);
            }
        }

        if (messages.isEmpty()) {
            return failed;
        }

        try {
            mailSender.send(messages.toArray(new MimeMessage[0]));
        } catch (MailSendException e) {
            int batchFailed = e.getFailedMessages().isEmpty() ? messages.size() : e.getFailedMessages().size();
            failed += batchFailed;
            log.warn("Newsletter batch partially failed: {}/{} messages", batchFailed, messages.size(), e);
        } catch (MailException e) {
            failed += messages.size();
            log.error("Newsletter batch failed entirely ({} messages)", messages.size(), e);
        }
        return failed;
    }

    String unsubscribePageUrl(String unsubscribeToken) {
        return siteUrl + "/newsletter/unsubscribe?token=" + unsubscribeToken;
    }

    private String oneClickUnsubscribeUrl(String unsubscribeToken) {
        return apiUrl + "/api/newsletter/unsubscribe/one-click?token=" + unsubscribeToken;
    }

    /** 템플릿 오류(IllegalStateException)도 삼켜서 구독 처리 트랜잭션을 롤백시키지 않는다. */
    private void sendNoticeQuietly(String email, String subject, String template, Map<String, String> variables) {
        try {
            mailSender.send(buildPlainMessage(email, subject, templates.render(template, variables)));
        } catch (MailException | IllegalStateException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Failed to send newsletter notice to {}", LogSanitizer.maskEmail(email), e);
        }
    }

    private MimeMessage buildPlainMessage(String to, String subject, String text)
        throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(fromEmail, senderName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, false);
        return message;
    }

    MimeMessage buildIssueMessage(String to, String unsubscribeToken, String subject, String content)
        throws MessagingException, UnsupportedEncodingException {
        String unsubscribeUrl = unsubscribePageUrl(unsubscribeToken);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(fromEmail, senderName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(renderText(content, unsubscribeUrl), renderHtml(content, unsubscribeUrl));

        // RFC 8058 one-click unsubscribe (Gmail/Yahoo 등에서 '구독 취소' 버튼 노출)
        message.setHeader("List-Unsubscribe", "<" + oneClickUnsubscribeUrl(unsubscribeToken) + ">");
        message.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
        return message;
    }

    private String renderText(String content, String unsubscribeUrl) {
        return templates.render(ISSUE_TEXT_TEMPLATE, Map.of(
            "content", content.strip(),
            "unsubscribeUrl", unsubscribeUrl,
            "senderName", senderName,
            "siteUrl", siteUrl));
    }

    String renderHtml(String content, String unsubscribeUrl) {
        return templates.render(ISSUE_HTML_TEMPLATE, Map.of(
            "contentHtml", contentToHtml(content),
            "unsubscribeUrl", unsubscribeUrl,
            "senderName", senderName,
            "siteUrl", siteUrl));
    }

    /** 일반 텍스트 본문을 이스케이프하고 URL은 링크로, 줄바꿈은 &lt;br&gt;로 바꾼다. */
    static String contentToHtml(String content) {
        String text = content.strip().replace("\r\n", "\n");
        Matcher matcher = URL_PATTERN.matcher(text);
        StringBuilder html = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            html.append(HtmlUtils.htmlEscape(text.substring(last, matcher.start())));
            String url = HtmlUtils.htmlEscape(matcher.group());
            html.append("<a href=\"").append(url).append("\" style=\"color:#4f46e5;\">").append(url).append("</a>");
            last = matcher.end();
        }
        html.append(HtmlUtils.htmlEscape(text.substring(last)));
        return html.toString().replace("\n", "<br>");
    }
}
