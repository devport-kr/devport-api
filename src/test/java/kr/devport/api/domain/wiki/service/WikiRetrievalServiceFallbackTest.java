package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WikiRetrievalServiceFallbackTest {

    @Test
    @DisplayName("retrieval context exposes weak-grounding and next-question seams")
    void retrievalContext_exposesWeakGroundingAndNextQuestionSeams() {
        WikiRetrievedChunk chunk = new WikiRetrievedChunk(
                "architecture",
                "auth",
                "Authentication Flow",
                "JWT filter and refresh handling",
                0.84,
                0.62,
                "src/main/java/.../SecurityConfig.java"
        );
        WikiRetrievalContext context = new WikiRetrievalContext(
                "github:repo",
                "인증 흐름은 JWT 필터 중심입니다.",
                true,
                true,
                List.of(chunk),
                List.of("JWT 필터가 적용되는 경로를 알려줘", "리프레시 토큰 흐름을 설명해줘")
        );

        assertThat(context.hasGrounding()).isTrue();
        assertThat(context.weakGrounding()).isTrue();
        assertThat(context.chunks()).singleElement().extracting(WikiRetrievedChunk::heading)
                .isEqualTo("Authentication Flow");
        assertThat(context.suggestedNextQuestions()).hasSize(2);
    }

    @Test
    @DisplayName("partial retrieval fallback returns short grounded guidance with better next questions")
    void retrieveContext_returnsShortGroundedGuidanceWhenGroundingIsWeak() {
        assertThat(new WikiRetrievalContext(
                "github:repo",
                "약한 근거만 확보됨",
                true,
                true,
                List.of(),
                List.of("아키텍처 섹션 기준으로 설명해줘", "현재 인증 클래스 경로를 알려줘")
        ).suggestedNextQuestions()).hasSizeBetween(2, 3);
    }

    @Test
    @DisplayName("retrieval reranking favors diverse sections over duplicate chunk clusters")
    void retrieveContext_favorsDiverseSectionsOverDuplicateClusters() {
        List<WikiRetrievedChunk> chunks = List.of(
                new WikiRetrievedChunk("architecture", null, "Architecture", "...", 0.91, 0.80, "a"),
                new WikiRetrievedChunk("how-it-works", null, "How It Works", "...", 0.89, 0.82, "b")
        );

        assertThat(chunks)
                .extracting(WikiRetrievedChunk::sectionId)
                .containsExactly("architecture", "how-it-works");
    }
}
