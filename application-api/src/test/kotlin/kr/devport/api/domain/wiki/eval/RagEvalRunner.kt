package kr.devport.api.domain.wiki.eval

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kr.devport.api.domain.wiki.service.WikiRetrievalService
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * Opt-in RAG evaluation harness (real Postgres + OpenAI). Run with `./gradlew :application-api:ragEval`.
 */
@Tag("rag-eval")
@SpringBootTest
@ActiveProfiles("rag-eval")
@EnabledIfSystemProperty(named = "rag-eval", matches = "true")
class RagEvalRunner {
    @Autowired
    private lateinit var retrievalService: WikiRetrievalService

    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Test
    fun runEvaluation() {
        val databaseUrl = System.getenv("RAG_EVAL_DATABASE_URL")
        if (databaseUrl.isNullOrBlank()) {
            throw IllegalStateException("RAG_EVAL_DATABASE_URL must be set for ragEval.")
        }

        val fixtures: List<Fixture> = objectMapper.readValue(FIXTURES_PATH.toFile(), object : TypeReference<List<Fixture>>() {})
        if (fixtures.isEmpty()) {
            throw IllegalStateException("No rag-eval fixtures found. Populate src/test/resources/rag-eval/fixtures.json.")
        }

        val questionResults = mutableListOf<QuestionResult>()
        var skipped = 0

        for (fixture in fixtures) {
            try {
                val chunks = retrievalService.retrieveContext(fixture.projectExternalId, fixture.question).chunks ?: emptyList()
                questionResults.add(scoreFixture(fixture, chunks))
            } catch (e: IllegalArgumentException) {
                skipped++
                questionResults.add(QuestionResult.skipped(fixture, e.message))
            }
        }

        val summary = summarize(questionResults, skipped)
        Files.createDirectories(OUTPUT_PATH.parent)
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(OUTPUT_PATH.toFile(), ResultEnvelope(summary, questionResults))

        System.out.printf(
            Locale.ROOT,
            "rag-eval fixtures=%d evaluated=%d skipped=%d hit@1=%.3f hit@3=%.3f hit@5=%.3f mrr=%.3f%n",
            fixtures.size,
            summary.evaluated,
            summary.skipped,
            summary.hitAt1,
            summary.hitAt3,
            summary.hitAt5,
            summary.mrr,
        )
    }

    private fun scoreFixture(
        fixture: Fixture,
        chunks: List<kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk>,
    ): QuestionResult {
        val expectedSections = fixture.expectedSectionIds.toSet()
        val expectedChunkTypes = fixture.expectedChunkTypes.toSet()

        var firstRelevantRank = -1
        var hitAt1 = false
        var hitAt3 = false
        var hitAt5 = false

        for (i in chunks.indices) {
            val chunk = chunks[i]
            val matchesSection = expectedSections.contains(chunk.sectionId)
            val matchesChunkType = expectedChunkTypes.isEmpty() || expectedChunkTypes.contains(chunk.chunkType)

            if (matchesSection && matchesChunkType) {
                firstRelevantRank = i + 1
                hitAt1 = firstRelevantRank <= 1
                hitAt3 = firstRelevantRank <= 3
                hitAt5 = firstRelevantRank <= 5
                break
            }
        }

        val reciprocalRank = if (firstRelevantRank > 0) 1.0 / firstRelevantRank else 0.0
        return QuestionResult(
            fixture.projectExternalId,
            fixture.question,
            fixture.expectedSectionIds,
            fixture.expectedChunkTypes,
            chunks.map { chunk ->
                RetrievedChunkResult(
                    chunk.sectionId,
                    chunk.chunkType,
                    chunk.heading,
                    chunk.similarityScore,
                    chunk.rerankScore,
                )
            },
            hitAt1,
            hitAt3,
            hitAt5,
            reciprocalRank,
            firstRelevantRank,
            false,
            null,
        )
    }

    private fun summarize(
        results: List<QuestionResult>,
        skipped: Int,
    ): Summary {
        val evaluated = results.filter { !it.skipped }
        val evaluatedCount = evaluated.size
        if (evaluatedCount == 0) {
            return Summary(0, skipped, 0.0, 0.0, 0.0, 0.0)
        }

        val hitAt1 = evaluated.count { it.hitAt1 } / evaluatedCount.toDouble()
        val hitAt3 = evaluated.count { it.hitAt3 } / evaluatedCount.toDouble()
        val hitAt5 = evaluated.count { it.hitAt5 } / evaluatedCount.toDouble()
        val mrr = evaluated.map { it.reciprocalRank }.average()
        return Summary(evaluatedCount, skipped, hitAt1, hitAt3, hitAt5, mrr)
    }

    private data class Fixture(
        val projectExternalId: String = "",
        val question: String = "",
        val expectedSectionIds: List<String> = emptyList(),
        val expectedChunkTypes: List<String> = emptyList(),
    )

    private data class RetrievedChunkResult(
        val sectionId: String?,
        val chunkType: String?,
        val heading: String?,
        val similarityScore: Double,
        val rerankScore: Double?,
    )

    private data class QuestionResult(
        val projectExternalId: String,
        val question: String,
        val expectedSectionIds: List<String>,
        val expectedChunkTypes: List<String>,
        val retrievedChunks: List<RetrievedChunkResult>,
        val hitAt1: Boolean,
        val hitAt3: Boolean,
        val hitAt5: Boolean,
        val reciprocalRank: Double,
        val firstRelevantRank: Int,
        val skipped: Boolean,
        val skipReason: String?,
    ) {
        companion object {
            fun skipped(
                fixture: Fixture,
                reason: String?,
            ): QuestionResult =
                QuestionResult(
                    fixture.projectExternalId,
                    fixture.question,
                    fixture.expectedSectionIds,
                    fixture.expectedChunkTypes,
                    emptyList(),
                    false,
                    false,
                    false,
                    0.0,
                    -1,
                    true,
                    reason,
                )
        }
    }

    private data class Summary(
        val evaluated: Int,
        val skipped: Int,
        val hitAt1: Double,
        val hitAt3: Double,
        val hitAt5: Double,
        val mrr: Double,
    )

    private data class ResultEnvelope(
        val summary: Summary,
        val questions: List<QuestionResult>,
    )

    companion object {
        private val FIXTURES_PATH: Path = Path.of("src/test/resources/rag-eval/fixtures.json")
        private val OUTPUT_PATH: Path = Path.of("build/rag-eval/results.json")
    }
}
