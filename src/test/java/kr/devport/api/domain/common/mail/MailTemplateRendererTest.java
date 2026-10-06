package kr.devport.api.domain.common.mail;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailTemplateRendererTest {

    private final MailTemplateRenderer renderer = new MailTemplateRenderer();

    @Test
    void insertsValuesLiterallyWithoutReinterpretingThem() {
        String text = renderer.render("newsletter/issue.txt", Map.of(
            "content", "가격 $1 \\ {{senderName}}",
            "unsubscribeUrl", "https://devport.kr/newsletter/unsubscribe?token=t",
            "senderName", "devport",
            "siteUrl", "https://devport.kr"));

        assertThat(text)
            .startsWith("가격 $1 \\ {{senderName}}\n")
            .contains("발신: devport (https://devport.kr)");
    }

    @Test
    void missingVariableFails() {
        assertThatThrownBy(() -> renderer.render("newsletter/verification.txt", Map.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("confirmUrl");
    }

    @Test
    void missingTemplateFails() {
        assertThatThrownBy(() -> renderer.render("newsletter/nope.txt", Map.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("templates/mail/newsletter/nope.txt");
    }

    /** native image에는 RuntimeHints로 등록된 리소스만 들어가므로, 모든 템플릿이 패턴에 걸리는지 확인한다. */
    @Test
    void everyTemplateIsRegisteredForNativeImage() throws IOException {
        RuntimeHints hints = new RuntimeHints();
        new MailTemplateRenderer.MailTemplateRuntimeHints().registerHints(hints, getClass().getClassLoader());

        Resource[] templates = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:" + MailTemplateRenderer.TEMPLATE_ROOT + "**/*.*");

        assertThat(templates).isNotEmpty();
        for (Resource template : templates) {
            String url = template.getURL().toString();
            String path = url.substring(url.indexOf(MailTemplateRenderer.TEMPLATE_ROOT));
            assertThat(RuntimeHintsPredicates.resource().forResource(path).test(hints))
                .as("native resource hint for %s", path)
                .isTrue();
        }
    }
}
