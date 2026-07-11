package kr.devport.api

import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.infrastructure.WikiSectionChunkRepository
import kr.devport.api.domain.wiki.repository.WikiSectionChunkJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Full composition-root boot test against real Postgres (pgvector) + Redis via Testcontainers.
 * Skipped automatically when Docker is unavailable, so the default build stays green without Docker.
 *
 * Covers the integration gate the strict-hexagonal refactor needed: the whole context (auth, article,
 * gitrepo, llm, mypage, port, wiki + all adapters) wires together, and the wiki pgvector/pg_trgm
 * search SQL — which H2 could never model — actually round-trips through the repository port.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextLoadTest {
    @Autowired
    private lateinit var chunkRepository: WikiSectionChunkRepository

    @Autowired
    private lateinit var chunkJpaRepository: WikiSectionChunkJpaRepository

    @Test
    fun contextLoads() {
        // The @SpringBootTest context starting successfully is the assertion.
    }

    @Test
    fun `wiki vector and lexical search round-trip through the repository port`() {
        val embedding = FloatArray(EMBEDDING_DIMS) { 0.0123f }
        val saved =
            WikiSectionChunk().apply {
                projectExternalId = PROJECT
                sectionId = "sec-1"
                chunkType = "summary"
                content = "authentication flow handled by SecurityConfig and JwtAuthenticationFilter"
                this.embedding = embedding
                commitSha = "test-commit"
            }
        chunkJpaRepository.save(saved)

        val vectorQuery = embedding.joinToString(",", prefix = "[", postfix = "]")
        val vectorHits = chunkRepository.findSimilarChunksWithScore(PROJECT, vectorQuery, 5)
        assertThat(vectorHits).isNotEmpty
        assertThat(vectorHits.first().chunk.projectExternalId).isEqualTo(PROJECT)
        // Identical vector → cosine distance ~0 → score ~1.0.
        assertThat(vectorHits.first().score).isGreaterThan(0.99)

        val lexicalHits =
            chunkRepository.findLexicalCandidates(
                PROJECT,
                "authentication flow handled by SecurityConfig and JwtAuthenticationFilter",
                5,
            )
        assertThat(lexicalHits).isNotEmpty
        assertThat(lexicalHits.first().chunk.projectExternalId).isEqualTo(PROJECT)
    }

    companion object {
        private const val PROJECT = "github:contextload-test"
        private const val EMBEDDING_DIMS = 1536

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
