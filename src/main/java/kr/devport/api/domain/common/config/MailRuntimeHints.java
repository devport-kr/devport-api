package kr.devport.api.domain.common.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Jakarta Mail(Angus)은 mailcap을 읽어 MIME 타입별 DataContentHandler를 리플렉션으로 생성한다.
 * native image에서 text/plain, text/html, multipart 메일 발송이
 * "no object DCH for MIME type" 로 실패하지 않도록 핸들러와 mailcap 리소스를 등록한다.
 */
public class MailRuntimeHints implements RuntimeHintsRegistrar {

    private static final String[] CONTENT_HANDLERS = {
        "org.eclipse.angus.mail.handlers.text_plain",
        "org.eclipse.angus.mail.handlers.text_html",
        "org.eclipse.angus.mail.handlers.text_xml",
        "org.eclipse.angus.mail.handlers.multipart_mixed",
        "org.eclipse.angus.mail.handlers.message_rfc822",
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (String handler : CONTENT_HANDLERS) {
            hints.reflection().registerTypeIfPresent(classLoader, handler, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        }
        hints.resources().registerPattern("META-INF/mailcap");
        hints.resources().registerPattern("META-INF/jakarta.mailcap");
        hints.resources().registerPattern("META-INF/mailcap.default");
        hints.resources().registerPattern("META-INF/mimetypes.default");
    }
}
