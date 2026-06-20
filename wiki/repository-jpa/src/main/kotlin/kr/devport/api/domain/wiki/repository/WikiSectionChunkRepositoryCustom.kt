package kr.devport.api.domain.wiki.repository

import kr.devport.api.domain.wiki.entity.WikiSectionChunk

interface WikiSectionChunkRepositoryCustom {
    fun findSimilarChunksWithScore(
        projectExternalId: String,
        queryEmbedding: String,
        limit: Int,
    ): List<ScoredChunkRow>

    fun findSimilarChunksGlobalWithScore(
        queryEmbedding: String,
        limit: Int,
    ): List<ScoredChunkRow>

    fun findLexicalCandidates(
        projectExternalId: String,
        question: String,
        limit: Int,
    ): List<ScoredChunkRow>

    data class ScoredChunkRow(
        val chunk: WikiSectionChunk,
        val score: Double,
    )
}
