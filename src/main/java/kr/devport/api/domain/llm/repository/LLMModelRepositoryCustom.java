package kr.devport.api.domain.llm.repository;

import kr.devport.api.domain.llm.entity.LLMModel;
import kr.devport.api.domain.llm.dto.request.LLMModelSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LLMModelRepositoryCustom {

    /**
     * QueryDSL을 사용한 동적 검색 (페이지네이션)
     * - 타입 안전한 쿼리 빌딩
     * - 선택적 필터 조건 지원
     * - LEFT JOIN FETCH로 N+1 방지
     */
    Page<LLMModel> searchWithCondition(LLMModelSearchCondition condition, Pageable pageable);

    /**
     * QueryDSL을 사용한 동적 검색 (전체 목록)
     * - 리더보드 계산 등에 활용
     */
    List<LLMModel> findAllWithCondition(LLMModelSearchCondition condition);
}
