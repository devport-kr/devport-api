package kr.devport.api.repository;

import kr.devport.api.domain.entity.LLMBenchmarkScore;
import kr.devport.api.domain.enums.BenchmarkType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LLMBenchmarkScoreRepository extends JpaRepository<LLMBenchmarkScore, Long> {

    // Find all scores for a specific benchmark type, ordered by rank
    List<LLMBenchmarkScore> findByBenchmarkTypeOrderByRankAsc(BenchmarkType benchmarkType);

    // Find top N scores for a specific benchmark type
    List<LLMBenchmarkScore> findByBenchmarkTypeOrderByRankAsc(BenchmarkType benchmarkType, Pageable pageable);

    // Find all scores for a specific model
    List<LLMBenchmarkScore> findByModelId(Long modelId);

    // Find score for a specific model and benchmark type
    @Query("SELECT s FROM LLMBenchmarkScore s WHERE s.model.id = :modelId AND s.benchmarkType = :benchmarkType")
    LLMBenchmarkScore findByModelIdAndBenchmarkType(@Param("modelId") Long modelId, @Param("benchmarkType") BenchmarkType benchmarkType);
}
