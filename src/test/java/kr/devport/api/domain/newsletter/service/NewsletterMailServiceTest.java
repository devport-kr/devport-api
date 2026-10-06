package kr.devport.api.domain.newsletter.service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import kr.devport.api.domain.common.exception.EmailDeliveryException;
import kr.devport.api.domain.common.mail.MailTemplateRenderer;
import kr.devport.api.domain.newsletter.entity.NewsletterSubscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewsletterMailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private NewsletterMailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new NewsletterMailService(mailSender, new MailTemplateRenderer());
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@devport.kr");
        ReflectionTestUtils.setField(mailService, "senderName", "devport");
        ReflectionTestUtils.setField(mailService, "siteUrl", "https://devport.kr");
        ReflectionTestUtils.setField(mailService, "apiUrl", "https://api.devport.kr");
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private NewsletterSubscription recipient(long id, String email) {
        return NewsletterSubscription.builder().id(id).email(email).unsubscribeToken("unsub-" + id).build();
    }

    @Test
    void contentToHtmlEscapesMarkupAndLinksUrls() {
        String html = NewsletterMailService.contentToHtml(
            "<script>alert(1)</script>\n이번 주 글: https://devport.kr/articles/1?a=1&b=2.\n\"https://x.dev\"");

        assertThat(html)
            .doesNotContain("<script>")
            .contains("&lt;script&gt;")
            .contains("<a href=\"https://devport.kr/articles/1?a=1&amp;b=2\"")
            .contains("</a>.<br>")
            .contains("&quot;<a href=\"https://x.dev\"");
    }

    @Test
    void issueMessageCarriesOneClickUnsubscribeHeadersAndFooterLink() throws Exception {
        MimeMessage message = mailService.buildIssueMessage("reader@example.com", "tok123", "제목", "본문 https://devport.kr");

        assertThat(message.getHeader("List-Unsubscribe"))
            .containsExactly("<https://api.devport.kr/api/newsletter/unsubscribe/one-click?token=tok123>");
        assertThat(message.getHeader("List-Unsubscribe-Post")).containsExactly("List-Unsubscribe=One-Click");
        assertThat(((InternetAddress) message.getFrom()[0]).getPersonal()).isEqualTo("devport");
        assertThat(message.getSubject()).isEqualTo("제목");

        // multipart(text + html) 직렬화가 가능한지 확인 (content handler 동작)
        message.saveChanges();
        assertThatCode(() -> message.writeTo(new ByteArrayOutputStream())).doesNotThrowAnyException();

        assertThat(mailService.renderHtml("본문", mailService.unsubscribePageUrl("tok123")))
            .contains("https://devport.kr/newsletter/unsubscribe?token=tok123")
            .contains("수신거부");
    }

    @Test
    void issueHtmlTemplateEscapesSenderNameButKeepsRenderedContent() {
        ReflectionTestUtils.setField(mailService, "senderName", "<dev & port>");

        String html = mailService.renderHtml("<b>본문</b>\nhttps://devport.kr", mailService.unsubscribePageUrl("tok123"));

        assertThat(html)
            .contains("발신: &lt;dev &amp; port&gt;")
            .contains("&lt;b&gt;본문&lt;/b&gt;<br><a href=\"https://devport.kr\"")
            .doesNotContain("{{");
    }

    @Test
    void plainMailsAreRenderedFromTemplates() throws Exception {
        NewsletterSubscription subscription = recipient(1, "reader@example.com");
        subscription.setConsentedAt(LocalDateTime.of(2026, 10, 6, 9, 30));

        mailService.sendVerificationEmail("reader@example.com", "raw-token");
        mailService.sendSubscribedNotice(subscription);
        mailService.sendUnsubscribedNotice("reader@example.com", LocalDateTime.of(2026, 10, 7, 18, 5));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(3)).send(captor.capture());
        List<String> bodies = new ArrayList<>();
        for (MimeMessage message : captor.getAllValues()) {
            bodies.add((String) message.getContent());
        }

        assertThat(bodies).allSatisfy(body -> assertThat(body).doesNotContain("{{").endsWith("devport\n"));
        assertThat(bodies.get(0)).contains("https://devport.kr/newsletter/confirm?token=raw-token");
        assertThat(bodies.get(1))
            .contains("- 전송자: devport (https://devport.kr)")
            .contains("- 수신 동의 일시: 2026-10-06 09:30")
            .contains("https://devport.kr/newsletter/unsubscribe?token=unsub-1");
        assertThat(bodies.get(2)).contains("- 수신 거부 일시: 2026-10-07 18:05");
    }

    @Test
    void sendIssueBatchCountsFailedRecipients() {
        doThrow(new MailSendException(Map.of(new Object(), new RuntimeException("bounced"))))
            .when(mailSender).send(any(MimeMessage[].class));

        int failed = mailService.sendIssueBatch("제목", "본문", List.of(
            recipient(1, "a@example.com"), recipient(2, "b@example.com"), recipient(3, "c@example.com")));

        assertThat(failed).isEqualTo(1);
    }

    @Test
    void sendIssueBatchCountsWholeBatchOnConnectionFailure() {
        doThrow(new MailAuthenticationException("bad credentials")).when(mailSender).send(any(MimeMessage[].class));

        int failed = mailService.sendIssueBatch("제목", "본문", List.of(recipient(1, "a@example.com"), recipient(2, "b@example.com")));

        assertThat(failed).isEqualTo(2);
    }

    @Test
    void verificationMailFailureSurfacesAsEmailDeliveryException() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> mailService.sendVerificationEmail("reader@example.com", "raw"))
            .isInstanceOf(EmailDeliveryException.class);
    }

    @Test
    void noticeMailFailureIsSwallowed() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));
        NewsletterSubscription subscription = recipient(1, "reader@example.com");
        subscription.setConsentedAt(LocalDateTime.now());

        assertThatCode(() -> mailService.sendSubscribedNotice(subscription)).doesNotThrowAnyException();
        assertThatCode(() -> mailService.sendUnsubscribedNotice("reader@example.com", LocalDateTime.now()))
            .doesNotThrowAnyException();
    }
}
