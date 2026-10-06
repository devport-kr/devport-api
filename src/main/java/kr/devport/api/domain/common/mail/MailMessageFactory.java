package kr.devport.api.domain.common.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 모든 메일을 같은 모양으로 만든다.
 * {template}.txt는 text 파트, {template}.html은 공통 레이아웃(로고 헤더 + 본문 카드 + footer)에 넣어 html 파트가 된다.
 * 로고는 원격 이미지 차단과 무관하게 보이도록 inline(cid) 첨부로 넣는다.
 * 모든 템플릿에서 {@code {{senderName}}}, {@code {{siteUrl}}}(링크용), {@code {{siteHost}}}(표시용, 예: devport.kr)을 쓸 수 있다.
 */
@Component
@RequiredArgsConstructor
public class MailMessageFactory {

    public static final String DEFAULT_FOOTER = "footer.html";
    static final String LOGO_CONTENT_ID = "devport-logo";
    private static final String LAYOUT = "layout.html";
    private static final String LOGO_PATH = MailTemplateRenderer.TEMPLATE_ROOT + "logo.png";

    private final JavaMailSender mailSender;
    private final MailTemplateRenderer templates;
    private final Resource logo = loadLogo();

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.sender-name:devport}")
    private String senderName;

    @Value("${app.email.site-url}")
    private String siteUrl;

    public MimeMessage create(String to, String subject, String template, Map<String, String> variables)
        throws MessagingException, UnsupportedEncodingException {
        return create(to, subject, template, DEFAULT_FOOTER, variables);
    }

    /**
     * @param template {@code templates/mail/} 기준 경로, 확장자 제외 (예: {@code newsletter/verification})
     * @param footer   레이아웃 하단에 넣을 html 템플릿
     */
    public MimeMessage create(String to, String subject, String template, String footer, Map<String, String> variables)
        throws MessagingException, UnsupportedEncodingException {
        Map<String, String> vars = new HashMap<>(variables);
        vars.putIfAbsent("senderName", senderName);
        vars.putIfAbsent("siteUrl", siteUrl);
        vars.putIfAbsent("siteHost", siteHost(siteUrl));

        String html = templates.render(LAYOUT, Map.of(
            "body", templates.render(template + ".html", vars),
            "footer", templates.render(footer, vars),
            "siteUrl", siteUrl));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(fromEmail, senderName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(templates.render(template + ".txt", vars), html);
        // setText 이후에 추가해야 메일 클라이언트가 cid 참조를 찾는다
        helper.addInline(LOGO_CONTENT_ID, logo, "image/png");
        return message;
    }

    /** 화면에 보여줄 주소: https://devport.kr/ → devport.kr */
    static String siteHost(String siteUrl) {
        return siteUrl.replaceFirst("^https?://", "").replaceFirst("/+$", "");
    }

    private static Resource loadLogo() {
        try (InputStream in = new ClassPathResource(LOGO_PATH).getInputStream()) {
            return new ByteArrayResource(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Mail logo not found: " + LOGO_PATH, e);
        }
    }
}
