package kr.devport.api.domain.common.config

import kr.devport.api.domain.article.dto.response.ArticlePageResponse
import kr.devport.api.domain.article.dto.response.TrendingTickerResponse
import kr.devport.api.domain.wiki.dto.response.WikiProjectListResponse
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse
import kr.devport.api.llm.dto.response.LLMBenchmarkResponse
import kr.devport.api.llm.dto.response.LLMLeaderboardEntryResponse
import kr.devport.gitrepo.GitRepo
import kr.devport.gitrepo.GitRepoPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

/**
 * Every value that goes through a @Cacheable must survive the Redis serializer round trip. Guards the
 * "missing type id property '@class'" failure that made every cache hit return 500 for final Kotlin
 * data classes (article/gitrepo/llm/wiki). Values are built by reflection so nested/constructor-only
 * DTOs (no defaults) are covered too.
 */
class RedisCacheSerializerRoundTripTest {
    private val serializer = RedisConfig().jsonRedisSerializer()

    private fun assertRoundTrips(value: Any) {
        val restored = serializer.deserialize(serializer.serialize(value))
        assertThat(restored).isEqualTo(value)
    }

    @Test
    fun `article caches round-trip`() {
        assertRoundTrips(sample(ArticlePageResponse::class))
        assertRoundTrips(listOf(sample(TrendingTickerResponse::class)))
    }

    @Test
    fun `gitrepo caches round-trip`() {
        assertRoundTrips(sample(GitRepoPage::class))
        assertRoundTrips(listOf(sample(GitRepo::class)))
    }

    @Test
    fun `llm caches round-trip`() {
        assertRoundTrips(listOf(sample(LLMLeaderboardEntryResponse::class)))
        assertRoundTrips(listOf(sample(LLMBenchmarkResponse::class)))
    }

    @Test
    fun `wiki caches round-trip`() {
        assertRoundTrips(sample(WikiProjectListResponse::class))
        assertRoundTrips(sample(WikiProjectPageResponse::class))
    }

    // ---- reflection-based sample builder -------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> sample(kClass: KClass<T>): T {
        val ctor = kClass.primaryConstructor!!
        val args = ctor.parameters.associateWith { valueFor(it.type) }
        return ctor.callBy(args.filterValues { it !== SKIP }.mapKeys { it.key as KParameter }) as T
    }

    private object SKIP

    private fun valueFor(type: KType): Any? {
        val classifier = type.classifier as? KClass<*> ?: return null
        return when {
            classifier == String::class -> "x"
            classifier == Long::class -> 1L
            classifier == Int::class -> 1
            classifier == Double::class -> 1.5
            classifier == Float::class -> 1.5f
            classifier == Boolean::class -> true
            classifier == BigDecimal::class -> BigDecimal("1.50")
            classifier == LocalDateTime::class -> LocalDateTime.of(2026, 1, 2, 3, 4, 5)
            classifier == OffsetDateTime::class -> OffsetDateTime.parse("2026-01-02T03:04:05Z")
            classifier.java.isEnum -> classifier.java.enumConstants.first()
            classifier == List::class ->
                listOf(valueFor(type.arguments.first().type ?: return emptyList<Any>()))
            classifier == Map::class -> emptyMap<Any, Any>()
            classifier == Set::class -> emptySet<Any>()
            classifier.primaryConstructor != null -> sample(classifier)
            else -> null
        }
    }
}
