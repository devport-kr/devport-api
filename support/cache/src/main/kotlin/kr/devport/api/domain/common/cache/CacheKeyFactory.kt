package kr.devport.api.domain.common.cache

import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Canonical cache key factory for scoped cached reads. A Spring bean so SpEL in @Cacheable can
 * reference it via `@cacheKeyFactory.method(...)`. Domain enums are passed in as their String name
 * (SpEL `#category?.name()`) so this shared factory has no per-domain dependency.
 */
@Component
class CacheKeyFactory {
    // ===== Article =====

    fun articleListKey(
        category: String?,
        page: Int,
        size: Int,
    ): String {
        val categoryKey = if (!category.isNullOrBlank()) category else "all"
        return "%s_%d_%d".format(categoryKey, page, size)
    }

    fun trendingTickerKey(limit: Int): String = limit.toString()

    // ===== Git repository =====

    fun gitRepoListKey(
        category: String?,
        page: Int,
        size: Int,
    ): String {
        val categoryKey = if (!category.isNullOrBlank()) category else "all"
        return "%s_%d_%d".format(categoryKey, page, size)
    }

    fun trendingGitReposKey(
        page: Int,
        size: Int,
    ): String = "%d_%d".format(page, size)

    fun gitReposByLanguageKey(
        language: String?,
        limit: Int,
    ): String = "%s_%d".format(normalizeString(language), limit)

    // ===== LLM =====

    fun llmLeaderboardKey(
        benchmarkType: String?,
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
    ): String {
        val benchmarkKey = benchmarkType ?: "null"
        val providerKey = normalizeString(provider)
        val creatorKey = normalizeString(creatorSlug)
        val licenseKey = normalizeString(license)
        val priceKey = maxPrice?.toPlainString() ?: "null"
        val contextKey = minContextWindow?.toString() ?: "null"
        return "%s_%s_%s_%s_%s_%s".format(benchmarkKey, providerKey, creatorKey, licenseKey, priceKey, contextKey)
    }

    fun allBenchmarksKey(): String = "all"

    private fun normalizeString(value: String?): String {
        if (value.isNullOrBlank()) {
            return "all"
        }
        return value.trim().lowercase()
    }
}
