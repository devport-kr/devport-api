package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.infrastructure.EmbeddingPort
import kr.devport.api.domain.wiki.infrastructure.ScoredChunkRow
import kr.devport.api.domain.wiki.infrastructure.WikiSectionChunkRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

/**
 * Wiki chat retrieval using hybrid (vector + lexical RRF) retrieval over wiki_section_chunks,
 * followed by optional LLM reranking and diversity-aware greedy selection.
 */
@Service
class WikiRetrievalService(
    private val chunkRepository: WikiSectionChunkRepository,
    private val chunkReranker: WikiChunkReranker,
    private val embeddingPort: EmbeddingPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private enum class FaqType { PROBLEM_SOLVED, ARCHITECTURE, GETTING_STARTED, RECENT_CHANGES, KEY_FEATURES, NONE }

    fun retrieveContext(
        projectExternalId: String,
        userQuestion: String,
    ): WikiRetrievalContext {
        val allChunks = chunkRepository.findByProjectExternalId(projectExternalId)
        if (allChunks.isEmpty()) {
            throw IllegalArgumentException("No wiki content found for project: $projectExternalId")
        }

        return try {
            val faqType = detectFaq(userQuestion)
            val candidateLimit = if (faqType != FaqType.NONE) FAQ_CANDIDATE_LIMIT else CANDIDATE_LIMIT
            val vectorStr = toVectorString(embedText(userQuestion))

            val hybridCandidates = hybridRetrieve(projectExternalId, userQuestion, vectorStr, candidateLimit)
            if (hybridCandidates.isEmpty()) {
                buildWeakGroundingContext(projectExternalId, allChunks, userQuestion)
            } else {
                val strongestSignal = hybridCandidates.first()
                val weakGrounding =
                    strongestSignal.similarityScore < MIN_USEFUL_SIMILARITY && strongestSignal.lexicalScore <= 0.0
                if (weakGrounding) {
                    buildWeakGroundingContext(projectExternalId, allChunks, userQuestion)
                } else {
                    val selectedChunks = selectDiverseChunks(hybridCandidates, userQuestion, faqType)
                    WikiRetrievalContext(
                        projectExternalId = projectExternalId,
                        groundedContext = buildGroundedContext(selectedChunks),
                        hasGrounding = selectedChunks.isNotEmpty(),
                        weakGrounding = false,
                        chunks = toRetrievedChunks(selectedChunks),
                        suggestedNextQuestions = emptyList(),
                    )
                }
            }
        } catch (e: Exception) {
            log.warn("wiki-retrieval: retrieval failed for project {}: {}", projectExternalId, e.message)
            buildWeakGroundingContext(projectExternalId, allChunks, userQuestion)
        }
    }

    private fun hybridRetrieve(
        projectExternalId: String,
        question: String,
        queryEmbedding: String,
        vectorCandidateLimit: Int,
    ): List<HybridCandidate> {
        val vectorFuture =
            CompletableFuture.supplyAsync {
                chunkRepository.findSimilarChunksWithScore(projectExternalId, queryEmbedding, vectorCandidateLimit)
            }
        val lexicalFuture =
            CompletableFuture.supplyAsync {
                chunkRepository.findLexicalCandidates(projectExternalId, question, LEXICAL_CANDIDATE_LIMIT)
            }

        val vectorCandidates = vectorFuture.join()
        val lexicalCandidates = lexicalFuture.join()

        val fused = LinkedHashMap<String, HybridAccumulator>()
        mergeCandidates(fused, vectorCandidates, true)
        mergeCandidates(fused, lexicalCandidates, false)

        val fusedCandidates = fused.values.map { it.toCandidate() }.sortedByDescending { it.fusionScore }
        if (fusedCandidates.isEmpty()) {
            return emptyList()
        }

        val rerankInput =
            (0 until minOf(MAX_RERANK_CANDIDATES, fusedCandidates.size))
                .map { index -> fusedCandidates[index].withRerankIndex(index) }
        if (shouldSkipRerank(rerankInput)) {
            return rerankInput
        }
        val reranked = safeRerank(question, rerankInput)
        if (reranked.isEmpty()) {
            return rerankInput
        }

        val rerankScores = HashMap<Int, Double>()
        for (sc in reranked) {
            rerankScores.merge(sc.index, sc.score) { a, b -> maxOf(a, b) }
        }

        return rerankInput
            .map { candidate -> candidate.withRerankScore(rerankScores[candidate.rerankIndex]) }
            .sortedWith(
                compareByDescending<HybridCandidate> { it.effectiveRankingScore() }
                    .thenByDescending { it.fusionScore },
            )
    }

    private fun shouldSkipRerank(candidates: List<HybridCandidate>): Boolean {
        if (candidates.size <= 1) {
            return true
        }
        val first = candidates.first()
        val second = candidates[1]
        val strongTopMatch = first.similarityScore >= STRONG_MATCH_SIMILARITY
        val clearVectorGap = (first.similarityScore - second.similarityScore) >= CLEAR_WIN_SIMILARITY_GAP
        val lexicalDoesNotDisagree = first.lexicalScore <= 0.0 || first.lexicalScore >= second.lexicalScore
        return strongTopMatch && clearVectorGap && lexicalDoesNotDisagree
    }

    private fun safeRerank(
        question: String,
        candidates: List<HybridCandidate>,
    ): List<WikiChunkReranker.ScoredChunk> =
        try {
            chunkReranker.rerank(question, candidates.map { it.chunk })
        } catch (e: Exception) {
            log.warn("wiki-retrieval: reranker failed, falling back to fused order: {}", e.message)
            emptyList()
        }

    private fun mergeCandidates(
        fused: MutableMap<String, HybridAccumulator>,
        candidates: List<ScoredChunkRow>,
        vector: Boolean,
    ) {
        candidates.forEachIndexed { i, row ->
            val key = candidateKey(row.chunk)
            val accumulator = fused.getOrPut(key) { HybridAccumulator(row.chunk) }
            val rankContribution = 1.0 / (RRF_K + i + 1)
            if (vector) {
                accumulator.vectorScore = row.score
                accumulator.addFusionScore(rankContribution)
            } else {
                accumulator.lexicalScore = row.score
                accumulator.addFusionScore(rankContribution)
            }
        }
    }

    private fun buildWeakGroundingContext(
        projectExternalId: String,
        allChunks: List<WikiSectionChunk>,
        userQuestion: String,
    ): WikiRetrievalContext {
        val fallbackChunks = selectFallbackChunks(allChunks, userQuestion)
        return WikiRetrievalContext(
            projectExternalId = projectExternalId,
            groundedContext = buildGroundedContext(fallbackChunks),
            hasGrounding = fallbackChunks.isNotEmpty(),
            weakGrounding = true,
            chunks = toRetrievedChunks(fallbackChunks),
            suggestedNextQuestions = suggestNextQuestions(fallbackChunks),
        )
    }

    private fun selectDiverseChunks(
        candidates: List<HybridCandidate>,
        userQuestion: String,
        faqType: FaqType,
    ): List<ScoredChunk> {
        val scoredCandidates =
            candidates.map { candidate ->
                val headingScore = computeHeadingScore(candidate.chunk, userQuestion)
                val faqBonus = faqChunkBonus(faqType, candidate.chunk)
                val baseScore = candidate.effectiveRankingScore()
                ScoredChunk(
                    chunk = candidate.chunk,
                    similarityScore = candidate.similarityScore,
                    headingScore = headingScore,
                    rerankScore = candidate.rerankScore,
                    qualityScore = baseScore + headingScore + faqBonus,
                )
            }
        return greedySelect(scoredCandidates)
    }

    private fun selectFallbackChunks(
        allChunks: List<WikiSectionChunk>,
        userQuestion: String,
    ): List<ScoredChunk> {
        val scored =
            allChunks
                .sortedWith(
                    compareBy<WikiSectionChunk> { fallbackChunkPriority(it.chunkType) }
                        .thenBy { it.sectionId ?: "" }
                        .thenBy { it.subsectionId ?: "" },
                ).map { chunk ->
                    ScoredChunk(
                        chunk = chunk,
                        similarityScore = 0.35,
                        headingScore = computeHeadingScore(chunk, userQuestion),
                        rerankScore = null,
                        qualityScore = 0.35,
                    )
                }
        return greedySelect(scored)
    }

    private fun greedySelect(candidates: List<ScoredChunk>): List<ScoredChunk> {
        val remaining = candidates.toMutableList()
        val selected = mutableListOf<ScoredChunk>()
        val usedSections = LinkedHashSet<String>()

        while (remaining.isNotEmpty() && selected.size < MAX_CONTEXT_CHUNKS) {
            val next = remaining.maxByOrNull { rerankScore(it, usedSections) } ?: break
            selected.add(next)
            next.chunk.sectionId?.let { usedSections.add(it) }
            remaining.remove(next)
        }

        return selected
    }

    private fun rerankScore(
        candidate: ScoredChunk,
        usedSections: Set<String>,
    ): Double {
        val sid = candidate.chunk.sectionId
        val diversityBonus = if (sid != null && sid in usedSections) 0.0 else DIVERSITY_BONUS
        return candidate.qualityScore + diversityBonus
    }

    private fun fallbackChunkPriority(chunkType: String?): Int =
        when (chunkType) {
            "summary" -> 0
            "overview" -> 1
            else -> 2
        }

    private fun buildGroundedContext(chunks: List<ScoredChunk>): String {
        val context = StringBuilder()
        context.append("# Repository Context\n\n")
        for (chunk in chunks) {
            context.append("## ").append(resolveHeading(chunk.chunk)).append("\n\n")
            context.append(chunk.chunk.content).append("\n\n")
        }
        return truncateToTokenLimit(context.toString(), MAX_CONTEXT_TOKENS)
    }

    private fun toRetrievedChunks(chunks: List<ScoredChunk>): List<WikiRetrievedChunk> =
        chunks.map { chunk ->
            WikiRetrievedChunk(
                sectionId = chunk.chunk.sectionId,
                subsectionId = chunk.chunk.subsectionId,
                chunkType = chunk.chunk.chunkType,
                heading = resolveHeading(chunk.chunk),
                content = chunk.chunk.content,
                similarityScore = chunk.similarityScore,
                headingScore = chunk.headingScore,
                rerankScore = chunk.rerankScore,
                sourcePathHint = resolveSourcePathHint(chunk.chunk),
            )
        }

    private fun resolveHeading(chunk: WikiSectionChunk): String? {
        chunk.metadata?.get("titleKo")?.let { titleKo ->
            if (titleKo.toString().isNotBlank()) {
                return titleKo.toString()
            }
        }
        val sub = chunk.subsectionId
        if (!sub.isNullOrBlank()) {
            return "${chunk.sectionId} > $sub"
        }
        return chunk.sectionId
    }

    private fun resolveSourcePathHint(chunk: WikiSectionChunk): String? {
        val sourcePath = chunk.metadata?.get("sourcePath") ?: return null
        return sourcePath.toString().takeUnless { it.isBlank() }
    }

    private fun computeHeadingScore(
        chunk: WikiSectionChunk,
        userQuestion: String,
    ): Double {
        val questionTokens = tokenize(userQuestion)
        if (questionTokens.isEmpty()) {
            return 0.0
        }
        val headingTokens = tokenize("${resolveHeading(chunk)} ${chunk.sectionId}")
        val overlap = questionTokens.count { qt -> headingTokens.any { ht -> matchesToken(qt, ht) } }
        return overlap / questionTokens.size.toDouble()
    }

    private fun tokenize(text: String): Set<String> =
        TOKEN_SPLIT
            .split(text.lowercase(Locale.ROOT))
            .filter { it.length >= 2 }
            .toCollection(LinkedHashSet())

    private fun matchesToken(
        left: String,
        right: String,
    ): Boolean = left == right || left.startsWith(right) || right.startsWith(left)

    private fun detectFaq(question: String): FaqType =
        when {
            FAQ_PROBLEM.matcher(question).find() -> FaqType.PROBLEM_SOLVED
            FAQ_ARCH.matcher(question).find() -> FaqType.ARCHITECTURE
            FAQ_START.matcher(question).find() -> FaqType.GETTING_STARTED
            FAQ_CHANGES.matcher(question).find() -> FaqType.RECENT_CHANGES
            FAQ_FEATURES.matcher(question).find() -> FaqType.KEY_FEATURES
            else -> FaqType.NONE
        }

    private fun faqChunkBonus(
        faqType: FaqType,
        chunk: WikiSectionChunk,
    ): Double =
        when (faqType) {
            FaqType.PROBLEM_SOLVED, FaqType.KEY_FEATURES -> if (chunk.chunkType == "summary") SUMMARY_FAQ_BONUS else 0.0
            else -> 0.0
        }

    private fun suggestNextQuestions(chunks: List<ScoredChunk>): List<String> {
        val suggestions = LinkedHashSet<String>()
        for (chunk in chunks) {
            suggestions.add("${resolveHeading(chunk.chunk)} 기준으로 설명해줘")
            if (suggestions.size == 3) {
                break
            }
            val sourcePathHint = resolveSourcePathHint(chunk.chunk)
            if (sourcePathHint != null) {
                suggestions.add("$sourcePathHint 관련 흐름을 알려줘")
            }
            if (suggestions.size == 3) {
                break
            }
        }
        if (suggestions.size < 2) {
            suggestions.add("이 질문과 직접 관련된 클래스나 파일 경로를 알려줘")
        }
        if (suggestions.size < 2) {
            suggestions.add("관련 섹션 이름을 기준으로 다시 설명해줘")
        }
        if (suggestions.size < 3) {
            suggestions.add("질문을 더 좁혀서 어떤 부분이 궁금한지 알려줘")
        }
        return suggestions.take(3)
    }

    private fun candidateKey(chunk: WikiSectionChunk): String {
        val subsectionId = chunk.subsectionId ?: ""
        return "${chunk.projectExternalId}|${chunk.sectionId}|$subsectionId|${chunk.chunkType}"
    }

    private fun embedText(text: String): FloatArray = embeddingPort.embed(text)

    private fun toVectorString(vector: FloatArray): String = vector.joinToString(",", prefix = "[", postfix = "]")

    private fun truncateToTokenLimit(
        text: String,
        maxTokens: Int,
    ): String {
        val maxChars = maxTokens * 4
        return if (text.length <= maxChars) text else text.substring(0, maxChars) + "..."
    }

    private class HybridAccumulator(
        val chunk: WikiSectionChunk,
    ) {
        var vectorScore: Double = 0.0
        var lexicalScore: Double = 0.0
        var fusionScore: Double = 0.0

        fun addFusionScore(contribution: Double) {
            fusionScore += contribution
        }

        fun toCandidate(): HybridCandidate = HybridCandidate(chunk, vectorScore, lexicalScore, fusionScore, null, -1)
    }

    private data class HybridCandidate(
        val chunk: WikiSectionChunk,
        val similarityScore: Double,
        val lexicalScore: Double,
        val fusionScore: Double,
        val rerankScore: Double?,
        val rerankIndex: Int,
    ) {
        fun withRerankScore(value: Double?): HybridCandidate = copy(rerankScore = value)

        fun withRerankIndex(value: Int): HybridCandidate = copy(rerankIndex = value)

        fun effectiveRankingScore(): Double = rerankScore ?: fusionScore
    }

    private data class ScoredChunk(
        val chunk: WikiSectionChunk,
        val similarityScore: Double,
        val headingScore: Double,
        val rerankScore: Double?,
        val qualityScore: Double,
    )

    companion object {
        private const val CANDIDATE_LIMIT = 30
        private const val FAQ_CANDIDATE_LIMIT = 40
        private const val LEXICAL_CANDIDATE_LIMIT = 30
        private const val MAX_RERANK_CANDIDATES = 20
        private const val MAX_CONTEXT_CHUNKS = 4
        private const val MAX_CONTEXT_TOKENS = 3000
        private const val MIN_USEFUL_SIMILARITY = 0.30
        private const val STRONG_MATCH_SIMILARITY = 0.80
        private const val CLEAR_WIN_SIMILARITY_GAP = 0.08
        private const val DIVERSITY_BONUS = 0.35
        private const val SUMMARY_FAQ_BONUS = 0.5
        private const val RRF_K = 60
        private val TOKEN_SPLIT: Pattern = Pattern.compile("[^a-z0-9가-힣]+")

        private val FAQ_PROBLEM: Pattern =
            Pattern.compile(
                "문제.*해결|해결.*문제|왜 만들|어떤.*목적|what.*problem|solve|why.*built",
                Pattern.CASE_INSENSITIVE,
            )
        private val FAQ_ARCH: Pattern =
            Pattern.compile(
                "아키텍처|구조|설계|architecture|design|어떻게.*동작|동작.*원리|how.*work|내부.*구조",
                Pattern.CASE_INSENSITIVE,
            )
        private val FAQ_START: Pattern =
            Pattern.compile(
                "시작하려면|어떻게.*시작|설치|install|setup|getting.?started|사용.*방법|how.*use|how.*start",
                Pattern.CASE_INSENSITIVE,
            )
        private val FAQ_CHANGES: Pattern =
            Pattern.compile(
                "최근.*변경|변경.*사항|changelog|release|업데이트|최근.*업|새로운|recent.*change|what.*new",
                Pattern.CASE_INSENSITIVE,
            )
        private val FAQ_FEATURES: Pattern =
            Pattern.compile(
                "주요.*기능|기능.*무엇|특징|feature|capabilities|what.*can|뭘.*할|어떤.*기능",
                Pattern.CASE_INSENSITIVE,
            )
    }
}
