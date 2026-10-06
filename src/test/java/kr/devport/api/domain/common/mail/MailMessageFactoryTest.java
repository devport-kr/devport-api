package kr.devport.api.domain.common.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimePart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailMessageFactoryTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private MailMessageFactory factory;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        factory = MailTestSupport.messageFactory(mailSender);
    }

    @Test
    void htmlPartUsesLayoutWithInlineLogo() throws Exception {
        MimeMessage message = factory.create("reader@example.com", "제목", "newsletter/verification",
            Map.of("confirmUrl", "https://devport.kr/newsletter/confirm?token=t"));

        assertThat(((InternetAddress) message.getFrom()[0]).getAddress()).isEqualTo("noreply@devport.kr");
        assertThat(((InternetAddress) message.getFrom()[0]).getPersonal()).isEqualTo("devport");
        assertThat(message.getSubject()).isEqualTo("제목");

        String html = MailTestSupport.html(message);
        assertThat(html)
            .contains("src=\"cid:" + MailMessageFactory.LOGO_CONTENT_ID + "\"")
            .contains("href=\"https://devport.kr/newsletter/confirm?token=t\"")
            .contains("본 메일은 발신 전용입니다.")
            .contains("감사합니다,<br>devport.kr</p>")
            .contains("<a href=\"https://devport.kr\" style=\"color:#6b7280;\">devport.kr</a>")
            .doesNotContain("{{");
        assertThat(MailTestSupport.text(message))
            .contains("https://devport.kr/newsletter/confirm?token=t")
            .doesNotContain("{{");

        MimePart logo = MailTestSupport.inline(message, MailMessageFactory.LOGO_CONTENT_ID);
        assertThat(logo.isMimeType("image/png")).isTrue();
        assertThat(logo.getInputStream().readAllBytes())
            .isEqualTo(new ClassPathResource("templates/mail/logo.png").getContentAsByteArray());
    }

    @Test
    void variablesAreEscapedInHtmlButNotInText() throws Exception {
        MimeMessage message = factory.create("reader@example.com", "제목", "auth/verify-email",
            Map.of("name", "<b>Kim</b>", "verificationUrl", "https://devport.kr/verify-email?token=t"));

        assertThat(MailTestSupport.html(message)).contains("안녕하세요 &lt;b&gt;Kim&lt;/b&gt;님").doesNotContain("<b>Kim</b>");
        assertThat(MailTestSupport.text(message)).contains("안녕하세요 <b>Kim</b>님");
    }

    @Test
    void siteHostDropsSchemeAndTrailingSlash() {
        assertThat(MailMessageFactory.siteHost("https://devport.kr")).isEqualTo("devport.kr");
        assertThat(MailMessageFactory.siteHost("https://devport.kr/")).isEqualTo("devport.kr");
        assertThat(MailMessageFactory.siteHost("http://localhost:5173")).isEqualTo("localhost:5173");
    }

    @Test
    void customFooterReplacesDefault() throws Exception {
        MimeMessage message = factory.create("reader@example.com", "제목", "newsletter/issue", "newsletter/issue-footer.html", Map.of(
            "content", "본문",
            "contentHtml", "본문",
            "unsubscribeUrl", "https://devport.kr/newsletter/unsubscribe?token=t"));

        assertThat(MailTestSupport.html(message))
            .contains("<a href=\"https://devport.kr/newsletter/unsubscribe?token=t\" style=\"color:#6b7280;\">수신거부</a>")
            .contains(">devport.kr</a>")
            .doesNotContain("발신 전용");
    }
}
