package kr.devport.api.domain.article.repository;

import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.article.dto.request.ArticleSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ArticleRepositoryCustom {

    /**
     * QueryDSL을 사용한 동적 검색 (페이지네이션)
     * - 타입 안전한 쿼리 빌딩
     * - 8개 선택적 필터 조건 지원
     * - 키워드 검색 (한글/영문 제목)
     */
    Page<Article> searchWithCondition(ArticleSearchCondition condition, Pageable pageable);

    /**
     * 전체 텍스트 검색 (페이지네이션, 정확한 count) - 관리자 목록용
     */
    Page<Article> searchFulltext(String query, Pageable pageable);

    /**
     * 전체 텍스트 검색 결과 (count 쿼리 없음) - 제목 우선, 최신순
     */
    List<Article> searchFulltextContent(String query, Pageable pageable);

    /**
     * 전체 텍스트 검색 정확한 count (관리자 목록용)
     */
    Long countFulltextMatches(String query);

    /**
     * 전체 텍스트 검색 count - 최대 cap개까지만 센다 (흔한 검색어에서 조기 종료)
     */
    long countFulltextMatches(String query, int cap);

    /**
     * 자동완성: 제목만 검색 (pg_trgm GIN 인덱스 사용), 최신순
     */
    List<ArticleTitleSuggestionRow> findTitleSuggestions(String query, int limit);

    /**
     * 제목 매칭 count - 최대 cap개까지만 센다
     */
    long countTitleMatches(String query, int cap);
}
