package kr.devport.api.domain.wiki.dto.internal

data class WikiChatResult(
    val answer: String?,
    val isClarification: Boolean,
    val clarificationOptions: List<String>?,
    val suggestedNextQuestions: List<String>?,
    val usedPreviousContext: Boolean,
    val sessionReset: Boolean,
)

data class WikiGlobalChatResult(
    val answer: String?,
    val relatedProjects: List<RelatedProjectLlmOutput>?,
    val hasRelatedProjects: Boolean,
    val sessionReset: Boolean,
) {
    data class RelatedProjectLlmOutput(
        val projectExternalId: String?,
        val relevanceReason: String?,
    )
}

data class WikiRetrievedChunk(
    val sectionId: String?,
    val subsectionId: String?,
    val chunkType: String?,
    val heading: String?,
    val content: String?,
    val similarityScore: Double,
    val headingScore: Double,
    val rerankScore: Double?,
    val sourcePathHint: String?,
)

data class WikiRetrievalContext(
    val projectExternalId: String?,
    val groundedContext: String?,
    val hasGrounding: Boolean,
    val weakGrounding: Boolean,
    val chunks: List<WikiRetrievedChunk>?,
    val suggestedNextQuestions: List<String>?,
)

data class WikiGlobalRetrievalContext(
    val groundedContext: String?,
    val scoredProjects: List<ScoredProject>?,
    val hasGrounding: Boolean,
) {
    data class ScoredProject(
        val projectExternalId: String?,
        val score: Double,
        val topChunks: List<WikiRetrievedChunk>?,
    )
}
