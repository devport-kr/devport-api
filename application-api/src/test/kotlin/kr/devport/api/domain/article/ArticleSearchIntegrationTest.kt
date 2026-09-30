package kr.devport.api.domain.article

import jakarta.persistence.EntityManagerFactory
import kr.devport.api.domain.article.entity.Article
import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.enums.ItemType
import kr.devport.api.domain.article.infrastructure.ArticleRepository
import kr.devport.api.domain.article.repository.ArticleJpaRepository
import kr.devport.api.domain.article.service.ArticleService
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDateTime

/**
 * Article search against real Postgres + Redis: title-only autocomplete, LIKE-escape handling, capped
 * counts, the Redis-cached fulltext page round trip, and the tags N+1 fix.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = ["spring.jpa.properties.hibernate.generate_statistics=true"])
@ActiveProfiles("test")
class ArticleSearchIntegrationTest {
    @Autowired private lateinit var articleRepository: ArticleRepository

    @Autowired private lateinit var articleJpa: ArticleJpaRepository

    @Autowired private lateinit var articleService: ArticleService

    @Autowired private lateinit var cacheManager: CacheManager

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    @BeforeEach
    fun clean() {
        articleJpa.deleteAll()
        cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
    }

    private fun save(
        title: String,
        body: String? = null,
        tags: List<String> = emptyList(),
        ageDays: Long = 0,
    ): Article =
        articleRepository.save(
            Article().apply {
                itemType = ItemType.BLOG
                source = "test"
                category = Category.AI_LLM
                summaryKoTitle = title
                summaryKoBody = body
                titleEn = "en $title"
                url = "https://example.com/$title"
                score = 1
                this.tags = tags.toMutableList()
                createdAtSource = LocalDateTime.now().minusDays(ageDays)
                createdAt = LocalDateTime.now()
                updatedAt = LocalDateTime.now()
            },
        )

    @Test
    fun `title suggestions match titles only, case-insensitively, newest first`() {
        save("React 19 정식 출시", ageDays = 2)
        save("리액트 vs react 성능 비교", ageDays = 1)
        save("스프링 부트", body = "react 언급은 본문에만 있음")

        val rows = articleRepository.findTitleSuggestions("REACT", 5)

        assertThat(rows.map { it.summaryKoTitle }).containsExactly("리액트 vs react 성능 비교", "React 19 정식 출시")
        assertThat(rows.first().category).isEqualTo(Category.AI_LLM)
    }

    @Test
    fun `LIKE metacharacters in the query are treated literally`() {
        save("성능 100% 개선")
        save("성능 1000 개선")
        save("snake_case 이름")
        save("snakeXcase 이름")

        assertThat(articleRepository.findTitleSuggestions("100%", 5)).hasSize(1)
        assertThat(articleRepository.findTitleSuggestions("e_c", 5).map { it.summaryKoTitle }).containsExactly("snake_case 이름")
    }

    @Test
    fun `title count stops at the cap`() {
        repeat(8) { save("캐시 전략 $it") }

        assertThat(articleRepository.countTitleMatches("캐시", 101)).isEqualTo(8)
        assertThat(articleRepository.countTitleMatches("캐시", 3)).isEqualTo(3)
    }

    @Test
    fun `fulltext count matches the content query and honours the cap`() {
        save("제목 하나", body = "kubernetes 운영 노하우")
        save("Kubernetes 입문")
        save("무관한 글", body = "다른 내용")

        val pageable = org.springframework.data.domain.PageRequest.of(0, 20)
        val content = articleRepository.searchFulltextContent("KUBERNETES", pageable)

        assertThat(content).hasSize(2)
        assertThat(articleRepository.countFulltextMatches("KUBERNETES", 1001)).isEqualTo(2)
        assertThat(articleRepository.countFulltextMatches("kubernetes", 1)).isEqualTo(1)
    }

    @Test
    fun `cached fulltext page round-trips through Redis`() {
        repeat(3) { save("검색 테스트 $it", tags = listOf("a", "b")) }

        val first = articleService.searchFulltext("검색 테스트", 0, 20)
        val second = articleService.searchFulltext("  검색 테스트 ", 0, 20) // key is trimmed/lowercased

        assertThat(cacheManager.getCache("articleSearch")!!.get("검색 테스트_0_20")).isNotNull
        assertThat(second).isEqualTo(first)
        assertThat(first.totalElements).isEqualTo(3)
        assertThat(first.content!!.first().tags).containsExactly("a", "b")
    }

    @Test
    fun `a fulltext page loads tags in batches, not one query per article`() {
        repeat(20) { save("배치 로딩 $it", tags = listOf("t1", "t2")) }
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.clear()

        articleService.searchFulltext("배치 로딩", 0, 20)

        // 1 content query + 1 batched tags query (pre-fix: 1 + 20).
        assertThat(statistics.prepareStatementCount).isLessThan(5)
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(
                DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"),
            ).withInitScript("testcontainers/init-pgvector.sql")

        @Container
        @ServiceConnection(name = "redis")
        @JvmStatic
        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine")).apply { withExposedPorts(6379) }
    }
}
