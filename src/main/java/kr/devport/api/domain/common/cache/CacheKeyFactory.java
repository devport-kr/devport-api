package kr.devport.api.domain.common.cache;

import kr.devport.api.domain.llm.enums.BenchmarkType;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Canonical cache key factory for scoped cached reads.
 *
 * Registered as a Spring bean so SpEL expressions in @Cacheable annotations
 * can reference it via {@code @cacheKeyFactory.method()} — avoiding T() type
 * lookups that fail in GraalVM native images.
 */
@Component
@ImportRuntimeHints(CacheKeyFactory.CacheKeyFactoryRuntimeHints.class)
public class CacheKeyFactory {

    public static class CacheKeyFactoryRuntimeHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection().registerType(CacheKeyFactory.class, MemberCategory.INVOKE_PUBLIC_METHODS);
        }
    }

    // ========== Article Domain Keys ==========

    public String articleListKey(kr.devport.api.domain.article.enums.Category category, int page, int size) {
        String categoryKey = category != null ? category.name() : "all";
        return String.format("%s_%d_%d", categoryKey, page, size);
    }

    public String trendingTickerKey(int limit) {
        return String.valueOf(limit);
    }

    // ========== Git Repository Domain Keys ==========

    public String gitRepoListKey(kr.devport.api.domain.gitrepo.enums.Category category, int page, int size) {
        String categoryKey = category != null ? category.name() : "all";
        return String.format("%s_%d_%d", categoryKey, page, size);
    }

    public String trendingGitReposKey(int page, int size) {
        return String.format("%d_%d", page, size);
    }

    public String gitReposByLanguageKey(String language, int limit) {
        String normalizedLanguage = normalizeString(language);
        return String.format("%s_%d", normalizedLanguage, limit);
    }

    // ========== LLM Domain Keys ==========

    public String llmLeaderboardKey(
        BenchmarkType benchmarkType,
        String provider,
        String creatorSlug,
        String license,
        BigDecimal maxPrice,
        Long minContextWindow
    ) {
        String benchmarkKey = benchmarkType.name();
        String providerKey = normalizeString(provider);
        String creatorKey = normalizeString(creatorSlug);
        String licenseKey = normalizeString(license);
        String priceKey = maxPrice != null ? maxPrice.toPlainString() : "null";
        String contextKey = minContextWindow != null ? minContextWindow.toString() : "null";

        return String.format("%s_%s_%s_%s_%s_%s",
            benchmarkKey, providerKey, creatorKey, licenseKey, priceKey, contextKey);
    }

    public String allBenchmarksKey() {
        return "all";
    }

    // ========== Normalization Utilities ==========

    private String normalizeString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "all";
        }
        return value.trim().toLowerCase();
    }

    private String normalizeList(String... values) {
        if (values == null || values.length == 0) {
            return "all";
        }

        String normalized = Arrays.stream(values)
            .filter(v -> v != null && !v.trim().isEmpty())
            .map(String::trim)
            .map(String::toLowerCase)
            .sorted()
            .collect(Collectors.joining(","));

        return normalized.isEmpty() ? "all" : normalized;
    }
}
