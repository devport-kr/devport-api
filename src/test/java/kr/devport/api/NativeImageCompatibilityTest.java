package kr.devport.api;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * prod는 GraalVM native image라 Hibernate가 런타임에 프록시를 만들 수 없다(BytecodeProvider 'none').
 * JVM 테스트는 ByteBuddy 프록시가 항상 동작해 이 실패를 재현하지 못하므로, 프록시가 필요한 코드를 정적으로 막는다.
 */
class NativeImageCompatibilityTest {

    @Test
    void entitiesDoNotUseLazyToOneAssociations() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<String> violations = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("kr.devport.api")) {
            Class<?> entity = Class.forName(definition.getBeanClassName());
            for (Field field : entity.getDeclaredFields()) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                if ((manyToOne != null && manyToOne.fetch() == FetchType.LAZY)
                    || (oneToOne != null && oneToOne.fetch() == FetchType.LAZY)) {
                    violations.add(entity.getSimpleName() + "." + field.getName());
                }
            }
        }

        assertThat(violations)
            .as("LAZY to-one associations need runtime proxies, which the native image cannot create")
            .isEmpty();
    }

    @Test
    void codeDoesNotRequestEntityProxies() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String code = Files.readString(source);
                if (code.contains("getReferenceById(") || code.contains(".getReference(")) {
                    violations.add(source.toString());
                }
            }
        }

        assertThat(violations)
            .as("getReferenceById/getReference return proxies, which the native image cannot create — use findById")
            .isEmpty();
    }
}
