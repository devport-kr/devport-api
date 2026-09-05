package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class WikiRetrievalServiceFallbackTest {
    @Test
    @DisplayName("retrieval context exposes weak-grounding and next-question seams")
    fun retrievalContextExposesWeakGroundingAndNextQuestionSeams() {
        val chunk =
            WikiRetrievedChunk(
                "architecture",
                "auth",
                "body",
                "Authentication Flow",
                "JWT filter and refresh handling",
                0.84,
                0.62,
                null,
                "src/main/java/.../SecurityConfig.java",
            )
        val context =
            WikiRetrievalContext(
                "github:repo",
                "인증 흐름은 JWT 필터 중심입니다.",
                true,
                true,
                listOf(chunk),
                listOf("JWT 필터가 적용되는 경로를 알려줘", "리프레시 토큰 흐름을 설명해줘"),
            )

        assertThat(context.hasGrounding).isTrue()
        assertThat(context.weakGrounding).isTrue()
        assertThat(context.chunks!!.single().heading).isEqualTo("Authentication Flow")
        assertThat(context.suggestedNextQuestions).hasSize(2)
    }

    @Test
    @DisplayName("partial retrieval fallback returns short grounded guidance with better next questions")
    fun returnsShortGroundedGuidanceWhenGroundingIsWeak() {
        assertThat(
            WikiRetrievalContext(
                "github:repo",
                "약한 근거만 확보됨",
                true,
                true,
                emptyList(),
                listOf("아키텍처 섹션 기준으로 설명해줘", "현재 인증 클래스 경로를 알려줘"),
            ).suggestedNextQuestions,
        ).hasSizeBetween(2, 3)
    }

    @Test
    @DisplayName("retrieval reranking favors diverse sections over duplicate chunk clusters")
    fun favorsDiverseSectionsOverDuplicateClusters() {
        val chunks =
            listOf(
                WikiRetrievedChunk("architecture", null, "summary", "Architecture", "...", 0.91, 0.80, null, "a"),
                WikiRetrievedChunk("how-it-works", null, "summary", "How It Works", "...", 0.89, 0.82, null, "b"),
            )

        assertThat(chunks.map { it.sectionId }).containsExactly("architecture", "how-it-works")
    }
}
