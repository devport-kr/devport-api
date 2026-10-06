package kr.devport.api.domain.common.mail;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * classpath:templates/mail/ 아래의 메일 템플릿을 읽어 자리표시자를 채운다.
 * <ul>
 *   <li>{@code {{name}}}: 값을 넣는다. {@code .html} 템플릿에서는 HTML 이스케이프한다.</li>
 *   <li>{@code {{{name}}}}: 이미 안전한 HTML로 보고 그대로 넣는다.</li>
 * </ul>
 * 템플릿이 없거나 값이 빠진 자리표시자가 있으면 {@link IllegalStateException}을 던진다.
 * native image는 등록된 리소스만 포함하므로 템플릿 디렉터리를 RuntimeHints로 등록한다.
 */
@Component
@ImportRuntimeHints(MailTemplateRenderer.MailTemplateRuntimeHints.class)
public class MailTemplateRenderer {

    static final String TEMPLATE_ROOT = "templates/mail/";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\{(\\w+)}}}|\\{\\{(\\w+)}}");

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public static class MailTemplateRuntimeHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern(TEMPLATE_ROOT + "**");
        }
    }

    /**
     * @param name {@code templates/mail/} 기준 경로 (예: {@code newsletter/issue.html})
     */
    public String render(String name, Map<String, String> variables) {
        String template = cache.computeIfAbsent(name, MailTemplateRenderer::load);
        boolean html = name.endsWith(".html");

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            boolean raw = matcher.group(1) != null;
            String key = raw ? matcher.group(1) : matcher.group(2);
            String value = variables.get(key);
            if (value == null) {
                throw new IllegalStateException("Missing variable '" + key + "' for mail template " + name);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(html && !raw ? HtmlUtils.htmlEscape(value) : value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private static String load(String name) {
        try (InputStream in = new ClassPathResource(TEMPLATE_ROOT + name).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Mail template not found: " + TEMPLATE_ROOT + name, e);
        }
    }
}
