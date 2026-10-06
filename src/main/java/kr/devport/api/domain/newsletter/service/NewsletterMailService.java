package kr.devport.api.domain.newsletter.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import kr.devport.api.domain.common.exception.EmailDeliveryException;
import kr.devport.api.domain.common.logging.LogSanitizer;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 뉴스레터 관련 메일 발송.
 * 수신 동의/거부 처리 결과 안내 메일은 「정보통신망법」 제50조에 따른 통지이며, 실패해도 요청을 막지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterMailService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"']*[^\\s<>\"'.,;:!?)\\]]");

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.newsletter.sender-name:devport}")
    private String senderName;

    @Value("${app.newsletter.site-url}")
    private String siteUrl;

    @Value("${app.newsletter.api-url}")
    private String apiUrl;

    public void sendVerificationEmail(String email, String rawToken) {
        String confirmUrl = siteUrl + "/newsletter/confirm?token=" + rawToken;
        String text = "안녕하세요,\n\n"
            + "devport 뉴스레터 구독 신청이 접수되었습니다.\n"
            + "아래 링크에서 '구독 확인'을 누르면 이메일 인증과 함께 구독이 완료됩니다:\n"
            + confirmUrl + "\n\n"
            + "이 링크는 24시간 후에 만료됩니다.\n"
            + "본인이 신청하지 않았다면 이 메일을 무시하세요. 인증하지 않으면 뉴스레터는 발송되지 않습니다.\n\n"
            + "감사합니다,\n"
            + senderName;

        try {
            mailSender.send(buildPlainMessage(email, "[devport] 뉴스레터 구독 이메일 인증", text));
            log.debug("Newsletter verification email sent to {}", LogSanitizer.maskEmail(email));
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new EmailDeliveryException("인증 메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    public void sendSubscribedNotice(NewsletterSubscription subscription) {
        String text = "안녕하세요,\n\n"
            + "devport 뉴스레터 수신 동의가 처리되었습니다.\n\n"
            + "- 전송자: " + senderName + " (" + siteUrl + ")\n"
            + "- 수신 동의 일시: " + DATE_TIME_FORMAT.format(subscription.getConsentedAt()) + "\n"
            + "- 처리 결과: 구독 완료\n\n"
            + "더 이상 받고 싶지 않으시면 언제든 아래 링크에서 수신을 거부할 수 있습니다:\n"
            + unsubscribePageUrl(subscription.getUnsubscribeToken()) + "\n\n"
            + "감사합니다,\n"
            + senderName;
        sendNoticeQuietly(subscription.getEmail(), "[devport] 뉴스레터 구독이 완료되었습니다", text);
    }

    public void sendUnsubscribedNotice(String email, LocalDateTime unsubscribedAt) {
        String text = "안녕하세요,\n\n"
            + "devport 뉴스레터 수신 거부가 처리되었습니다.\n\n"
            + "- 전송자: " + senderName + " (" + siteUrl + ")\n"
            + "- 수신 거부 일시: " + DATE_TIME_FORMAT.format(unsubscribedAt) + "\n"
            + "- 처리 결과: 수신 거부 완료 (구독 정보 삭제)\n\n"
            + "앞으로 뉴스레터가 발송되지 않습니다. 다시 받아보시려면 마이페이지에서 구독을 신청해주세요.\n\n"
            + "감사합니다,\n"
            + senderName;
        sendNoticeQuietly(email, "[devport] 뉴스레터 수신 거부가 처리되었습니다", text);
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

    private void sendNoticeQuietly(String email, String subject, String text) {
        try {
            mailSender.send(buildPlainMessage(email, subject, text));
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
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
        return content.strip() + "\n\n"
            + "--\n"
            + "본 메일은 devport 뉴스레터 수신에 동의하신 분께 발송되었습니다.\n"
            + "수신을 원하지 않으시면 아래 링크에서 수신을 거부할 수 있습니다:\n"
            + unsubscribeUrl + "\n"
            + "발신: " + senderName + " (" + siteUrl + ")";
    }

    String renderHtml(String content, String unsubscribeUrl) {
        String escapedUnsubscribeUrl = HtmlUtils.htmlEscape(unsubscribeUrl);
        String escapedSiteUrl = HtmlUtils.htmlEscape(siteUrl);
        return "<!DOCTYPE html><html lang=\"ko\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head>"
            + "<body style=\"margin:0;padding:0;background:#f4f5f7;\">"
            + "<div style=\"max-width:600px;margin:0 auto;padding:32px 16px;"
            + "font-family:-apple-system,BlinkMacSystemFont,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1f2328;\">"
            + "<div style=\"font-size:20px;font-weight:700;margin-bottom:24px;\">devport<span style=\"color:#6366f1;\">.</span></div>"
            + "<div style=\"background:#ffffff;border-radius:12px;padding:28px;font-size:15px;line-height:1.7;word-break:break-word;\">"
            + contentToHtml(content)
            + "</div>"
            + "<div style=\"margin-top:24px;font-size:12px;line-height:1.6;color:#6b7280;\">"
            + "본 메일은 devport 뉴스레터 수신에 동의하신 분께 발송되었습니다.<br>"
            + "수신을 원하지 않으시면 <a href=\"" + escapedUnsubscribeUrl + "\" style=\"color:#6b7280;\">수신거부</a>를 눌러주세요.<br>"
            + "발신: " + HtmlUtils.htmlEscape(senderName)
            + " · <a href=\"" + escapedSiteUrl + "\" style=\"color:#6b7280;\">" + escapedSiteUrl + "</a>"
            + "</div></div></body></html>";
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
