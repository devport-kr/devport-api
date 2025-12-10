package kr.devport.api.repository;

import kr.devport.api.domain.entity.LLMBenchmark;
import kr.devport.api.domain.enums.BenchmarkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LLMBenchmarkRepository extends JpaRepository<LLMBenchmark, BenchmarkType> {

    List<LLMBenchmark> findAllByOrderBySortOrderAsc();

    List<LLMBenchmark> findByCategoryGroupOrderBySortOrderAsc(String categoryGroup);
}
