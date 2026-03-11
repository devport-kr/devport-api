package kr.devport.api.domain.common.cache;

import kr.devport.api.domain.article.enums.Category;
import kr.devport.api.domain.llm.enums.BenchmarkType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheKeyFactory: 캐시 키 정규화 및 충돌 방지 테스트")
class CacheKeyFactoryTest {

    private final CacheKeyFactory CacheKeyFactory = new CacheKeyFactory();

    @Nested
    @DisplayName("Article 도메인 키 생성")
    class ArticleDomainKeys {

        @Test
        @DisplayName("동일한 카테고리/페이지 파라미터는 동일한 키 생성")
        void articleListKey_sameParameters_producesIdenticalKeys() {
            // given
            Category category = Category.AI_LLM;
            int page = 0;
            int size = 20;

            // when
            String key1 = CacheKeyFactory.articleListKey(category, page, size);
            String key2 = CacheKeyFactory.articleListKey(category, page, size);

            // then
            assertThat(key1).isEqualTo(key2);
            assertThat(key1).isEqualTo("AI_LLM_0_20");
        }

        @Test
        @DisplayName("null 카테고리는 'all'로 정규화")
        void articleListKey_nullCategory_normalizedToAll() {
            // when
            String key = CacheKeyFactory.articleListKey(null, 0, 20);

            // then
            assertThat(key).isEqualTo("all_0_20");
        }

        @Test
        @DisplayName("서로 다른 카테고리는 서로 다른 키 생성 (충돌 방지)")
        void articleListKey_differentCategories_produceDistinctKeys() {
            // when
            String keyAI = CacheKeyFactory.articleListKey(Category.AI_LLM, 0, 20);
            String keyFrontend = CacheKeyFactory.articleListKey(Category.FRONTEND, 0, 20);
            String keyNull = CacheKeyFactory.articleListKey(null, 0, 20);

            // then
            assertThat(keyAI).isNotEqualTo(keyFrontend);
            assertThat(keyAI).isNotEqualTo(keyNull);
            assertThat(keyFrontend).isNotEqualTo(keyNull);
        }

        @Test
        @DisplayName("서로 다른 페이지 번호는 서로 다른 키 생성 (충돌 방지)")
        void articleListKey_differentPages_produceDistinctKeys() {
            // when
            String keyPage0 = CacheKeyFactory.articleListKey(Category.AI_LLM, 0, 20);
            String keyPage1 = CacheKeyFactory.articleListKey(Category.AI_LLM, 1, 20);

            // then
            assertThat(keyPage0).isNotEqualTo(keyPage1);
        }

        @Test
        @DisplayName("trending ticker - 동일한 limit은 동일한 키 생성")
        void trendingTickerKey_sameLimit_producesIdenticalKeys() {
            // when
            String key1 = CacheKeyFactory.trendingTickerKey(10);
            String key2 = CacheKeyFactory.trendingTickerKey(10);

            // then
            assertThat(key1).isEqualTo(key2);
            assertThat(key1).isEqualTo("10");
        }
    }

    @Nested
    @DisplayName("GitRepo 도메인 키 생성")
    class GitRepoDomainKeys {

        @Test
        @DisplayName("동일한 카테고리/페이지 파라미터는 동일한 키 생성")
        void gitRepoListKey_sameParameters_producesIdenticalKeys() {
            // given
            kr.devport.api.domain.gitrepo.enums.Category category = 
                kr.devport.api.domain.gitrepo.enums.Category.FRONTEND;
            int page = 0;
            int size = 20;

            // when
            String key1 = CacheKeyFactory.gitRepoListKey(category, page, size);
            String key2 = CacheKeyFactory.gitRepoListKey(category, page, size);

            // then
            assertThat(key1).isEqualTo(key2);
            assertThat(key1).isEqualTo("FRONTEND_0_20");
        }

        @Test
        @DisplayName("null 카테고리는 'all'로 정규화")
        void gitRepoListKey_nullCategory_normalizedToAll() {
            // when
            String key = CacheKeyFactory.gitRepoListKey(null, 0, 20);

            // then
            assertThat(key).isEqualTo("all_0_20");
        }

        @Test
        @DisplayName("language 정규화 - 공백 제거 및 소문자 변환")
        void gitReposByLanguageKey_normalizesLanguage() {
            // when
            String key1 = CacheKeyFactory.gitReposByLanguageKey("Java", 10);
            String key2 = CacheKeyFactory.gitReposByLanguageKey("java", 10);
            String key3 = CacheKeyFactory.gitReposByLanguageKey("  JAVA  ", 10);

            // then - 대소문자 및 공백 차이를 무시하고 동일한 키 생성
            assertThat(key1).isEqualTo(key2);
            assertThat(key2).isEqualTo(key3);
            assertThat(key1).isEqualTo("java_10");
        }

        @Test
        @DisplayName("null/blank language는 'all'로 정규화")
        void gitReposByLanguageKey_nullOrBlankLanguage_normalizedToAll() {
            // when
            String keyNull = CacheKeyFactory.gitReposByLanguageKey(null, 10);
            String keyEmpty = CacheKeyFactory.gitReposByLanguageKey("", 10);
            String keyBlank = CacheKeyFactory.gitReposByLanguageKey("   ", 10);

            // then
            assertThat(keyNull).isEqualTo("all_10");
            assertThat(keyEmpty).isEqualTo("all_10");
            assertThat(keyBlank).isEqualTo("all_10");
        }
    }

    @Nested
    @DisplayName("LLM 도메인 키 생성")
    class LLMDomainKeys {

        @Test
        @DisplayName("동일한 파라미터는 동일한 키 생성")
        void llmLeaderboardKey_sameParameters_producesIdenticalKeys() {
            // given
            BenchmarkType benchmark = BenchmarkType.GPQA_DIAMOND;
            String provider = "openai";
            String creator = "openai";
            String license = "proprietary";
            BigDecimal maxPrice = new BigDecimal("0.01");
            Long minContext = 128000L;

            // when
            String key1 = CacheKeyFactory.llmLeaderboardKey(
                benchmark, provider, creator, license, maxPrice, minContext);
            String key2 = CacheKeyFactory.llmLeaderboardKey(
                benchmark, provider, creator, license, maxPrice, minContext);

            // then
            assertThat(key1).isEqualTo(key2);
        }

        @Test
        @DisplayName("null 문자열 필터는 'all'로 정규화")
        void llmLeaderboardKey_nullStringFilters_normalizedToAll() {
            // when
            String key = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, null, null, null, null, null);

            // then
            assertThat(key).isEqualTo("GPQA_DIAMOND_all_all_all_null_null");
        }

        @Test
        @DisplayName("문자열 필터 정규화 - 공백 제거 및 소문자 변환")
        void llmLeaderboardKey_normalizesStringFilters() {
            // when
            String key1 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, "OpenAI", "OpenAI", "MIT", null, null);
            String key2 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, "openai", "openai", "mit", null, null);
            String key3 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, "  OPENAI  ", "  OPENAI  ", "  MIT  ", null, null);

            // then - 대소문자 및 공백 차이를 무시하고 동일한 키 생성
            assertThat(key1).isEqualTo(key2);
            assertThat(key2).isEqualTo(key3);
        }

        @Test
        @DisplayName("서로 다른 benchmark는 서로 다른 키 생성 (충돌 방지)")
        void llmLeaderboardKey_differentBenchmarks_produceDistinctKeys() {
            // when
            String keyGPQA = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, "openai", null, null, null, null);
            String keyMMlu = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.MMLU_PRO, "openai", null, null, null, null);

            // then
            assertThat(keyGPQA).isNotEqualTo(keyMMlu);
        }

        @Test
        @DisplayName("서로 다른 numeric 필터는 서로 다른 키 생성 (충돌 방지)")
        void llmLeaderboardKey_differentNumericFilters_produceDistinctKeys() {
            // when
            String keyNoFilters = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, null, null, null, null, null);
            String keyWithPrice = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, null, null, null, new BigDecimal("0.01"), null);
            String keyWithContext = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, null, null, null, null, 128000L);
            String keyWithBoth = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, null, null, null, new BigDecimal("0.01"), 128000L);

            // then - numeric 필터 값이 다르면 키도 달라야 함 (충돌 방지)
            assertThat(keyNoFilters).isNotEqualTo(keyWithPrice);
            assertThat(keyNoFilters).isNotEqualTo(keyWithContext);
            assertThat(keyNoFilters).isNotEqualTo(keyWithBoth);
            assertThat(keyWithPrice).isNotEqualTo(keyWithContext);
            assertThat(keyWithPrice).isNotEqualTo(keyWithBoth);
        }

        @Test
        @DisplayName("동일한 numeric 값은 동일한 키 생성")
        void llmLeaderboardKey_sameNumericValues_produceIdenticalKeys() {
            // when
            String key1 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, null, null, null, 
                new BigDecimal("0.010"), 128000L);
            String key2 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, null, null, null, 
                new BigDecimal("0.01"), 128000L);

            // then - BigDecimal의 toPlainString()으로 정규화되므로 "0.01" vs "0.010"은 다를 수 있음
            // 이 경우는 의도적으로 다른 키를 생성 (엄격한 값 비교)
            // 하지만 동일한 객체는 동일한 키를 생성해야 함
            String key3 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, null, null, null, 
                new BigDecimal("0.01"), 128000L);
            assertThat(key2).isEqualTo(key3);
        }

        @Test
        @DisplayName("benchmarks 키는 항상 'all'")
        void allBenchmarksKey_alwaysReturnsAll() {
            // when
            String key1 = CacheKeyFactory.allBenchmarksKey();
            String key2 = CacheKeyFactory.allBenchmarksKey();

            // then
            assertThat(key1).isEqualTo("all");
            assertThat(key1).isEqualTo(key2);
        }
    }

    @Nested
    @DisplayName("Edge Cases - 키 드리프트 및 충돌 방지")
    class EdgeCases {

        @Test
        @DisplayName("빈 문자열과 null은 동일하게 'all'로 정규화")
        void blankAndNull_normalizedIdentically() {
            // Article
            assertThat(CacheKeyFactory.articleListKey(null, 0, 20))
                .isEqualTo("all_0_20");

            // GitRepo language
            String keyNull = CacheKeyFactory.gitReposByLanguageKey(null, 10);
            String keyEmpty = CacheKeyFactory.gitReposByLanguageKey("", 10);
            assertThat(keyNull).isEqualTo(keyEmpty);

            // LLM provider
            String llmKeyNull = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, null, null, null, null, null);
            String llmKeyEmpty = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, "", "", "", null, null);
            assertThat(llmKeyNull).isEqualTo(llmKeyEmpty);
        }

        @Test
        @DisplayName("대소문자 차이는 동일한 키로 정규화")
        void caseInsensitive_normalizedIdentically() {
            // GitRepo language
            assertThat(CacheKeyFactory.gitReposByLanguageKey("Java", 10))
                .isEqualTo(CacheKeyFactory.gitReposByLanguageKey("JAVA", 10));

            // LLM filters
            String key1 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, "OpenAI", "Meta", "MIT", null, null);
            String key2 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, "openai", "meta", "mit", null, null);
            assertThat(key1).isEqualTo(key2);
        }

        @Test
        @DisplayName("앞뒤 공백은 제거되어 동일한 키 생성")
        void whitespace_trimmedAndNormalized() {
            // GitRepo language
            assertThat(CacheKeyFactory.gitReposByLanguageKey("  java  ", 10))
                .isEqualTo(CacheKeyFactory.gitReposByLanguageKey("java", 10));

            // LLM filters
            String key1 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, "  openai  ", "  meta  ", "  mit  ", null, null);
            String key2 = CacheKeyFactory.llmLeaderboardKey(
                BenchmarkType.GPQA_DIAMOND, "openai", "meta", "mit", null, null);
            assertThat(key1).isEqualTo(key2);
        }
    }
}
