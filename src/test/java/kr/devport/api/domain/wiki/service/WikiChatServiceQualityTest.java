package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WikiChatServiceQualityTest {

    @Test
    @DisplayName("chat result exposes clarification and continuity seams for quality work")
    void chatResult_exposesClarificationAndContinuitySeams() {
        WikiChatResult result = new WikiChatResult(
                "요약하면 인증은 JWT 필터 중심이에요.",
                true,
                List.of("인증 흐름", "권한 체크", "리프레시 토큰"),
                List.of("SecurityConfig 경로를 알려줘", "JWT 필터 역할을 설명해줘"),
                true,
                false
        );

        assertThat(result.isClarification()).isTrue();
        assertThat(result.clarificationOptions()).hasSize(3);
        assertThat(result.usedPreviousContext()).isTrue();
        assertThat(result.sessionReset()).isFalse();
    }

    @Disabled("Enable in 06-04 when broad-answer-first orchestration is implemented")
    @Test
    @DisplayName("broad but answerable questions return a grounded summary before one narrow clarification")
    void chat_returnsGroundedSummaryBeforeOneNarrowClarification() {
        WikiChatResult result = new WikiChatResult(
                "요약하면 인증은 JWT 기반이에요. 어느 부분이 궁금한지 골라주세요.",
                true,
                List.of("로그인", "인가", "토큰 갱신"),
                List.of(),
                false,
                false
        );

        assertThat(result.answer()).startsWith("요약하면");
        assertThat(result.clarificationOptions()).hasSizeBetween(2, 3);
    }

    @Disabled("Enable in 06-04 when ambiguity-bullet behavior is implemented")
    @Test
    @DisplayName("ambiguous repo-area questions return short Korean clarification bullets")
    void chat_returnsShortKoreanClarificationBulletsForAmbiguousQuestions() {
        WikiChatResult result = new WikiChatResult(
                "질문이 넓어서 먼저 범위를 좁히면 더 정확하게 답할 수 있어요.",
                true,
                List.of("아키텍처", "빌드", "배포"),
                List.of(),
                false,
                false
        );

        assertThat(result.clarificationOptions()).allSatisfy(option -> assertThat(option).doesNotContain("?"));
    }
}
