package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
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

    @Test
    @DisplayName("broad but answerable questions return a grounded summary before one narrow clarification")
    void chat_returnsGroundedSummaryBeforeOneNarrowClarification() {
        WikiChatResult result = new WikiChatResult(
                "요약하면 인증 진입점은 SecurityConfig와 JwtAuthenticationFilter예요. 더 좁히면 로그인, 인가, 토큰 갱신 중 어디가 궁금한지 알려주세요.",
                true,
                List.of("로그인", "인가", "토큰 갱신"),
                List.of(),
                false,
                false
        );

        assertThat(result.answer()).startsWith("요약하면");
        assertThat(result.clarificationOptions()).hasSizeBetween(2, 3);
        assertThat(result.answer()).contains("SecurityConfig");
    }

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

    @Test
    @DisplayName("after two clarification turns the service answers the narrowest safe slice instead of looping")
    void chat_stopsClarificationLoopAfterTwoTurns() {
        WikiChatResult result = new WikiChatResult(
                "요약하면 현재 근거로는 JwtAuthenticationFilter 중심 흐름까지가 가장 안전해요.",
                false,
                List.of(),
                List.of("SecurityConfig 경로를 알려줘", "JWT 필터 호출 순서를 설명해줘"),
                true,
                false
        );

        assertThat(result.isClarification()).isFalse();
        assertThat(result.clarificationOptions()).isEmpty();
        assertThat(result.answer()).contains("가장 안전해요");
    }

    @Test
    @DisplayName("weak-grounding replies stay short and suggest better next questions")
    void chat_weakGroundingRepliesStayShortAndSuggestBetterNextQuestions() {
        WikiChatResult result = new WikiChatResult(
                "요약하면 지금은 deploy.yml 근거만 확인돼요.",
                false,
                List.of(),
                List.of("deploy.yml 기준으로 단계별로 설명해줘", "배포 secret 목록을 알려줘", "롤백 흐름을 설명해줘"),
                false,
                false
        );

        assertThat(result.answer()).doesNotContain("확신도");
        assertThat(result.suggestedNextQuestions()).hasSizeBetween(2, 3);
    }
}
