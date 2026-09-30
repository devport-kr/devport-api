package kr.devport.api.domain.llm.service.admin;

import kr.devport.api.domain.llm.entity.LLMBenchmark;
import kr.devport.api.domain.llm.enums.BenchmarkType;
import kr.devport.api.domain.llm.dto.request.admin.LLMBenchmarkCreateRequest;
import kr.devport.api.domain.llm.dto.request.admin.LLMBenchmarkUpdateRequest;
import kr.devport.api.domain.llm.dto.response.LLMBenchmarkResponse;
import kr.devport.api.domain.llm.repository.LLMBenchmarkRepository;
import lombok.RequiredArgsConstructor;
import kr.devport.api.domain.common.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LLMBenchmarkAdminService {

    private final LLMBenchmarkRepository llmBenchmarkRepository;

    @CacheEvict(value = CacheNames.LLM_BENCHMARKS, allEntries = true)
    public LLMBenchmarkResponse createLLMBenchmark(LLMBenchmarkCreateRequest request) {
        LLMBenchmark benchmark = LLMBenchmark.builder()
            .benchmarkType(request.getBenchmarkType())
            .displayName(request.getDisplayName())
            .categoryGroup(request.getCategoryGroup())
            .description(request.getDescription())
            .explanation(request.getExplanation())
            .sortOrder(request.getSortOrder())
            .createdAt(LocalDateTime.now())
            .build();

        LLMBenchmark saved = llmBenchmarkRepository.save(benchmark);
        return convertToResponse(saved);
    }

    @CacheEvict(value = CacheNames.LLM_BENCHMARKS, allEntries = true)
    public LLMBenchmarkResponse updateLLMBenchmark(BenchmarkType benchmarkType, LLMBenchmarkUpdateRequest request) {
        LLMBenchmark benchmark = llmBenchmarkRepository.findById(benchmarkType)
            .orElseThrow(() -> new IllegalArgumentException("LLMBenchmark not found with type: " + benchmarkType));

        if (request.getDisplayName() != null) benchmark.setDisplayName(request.getDisplayName());
        if (request.getCategoryGroup() != null) benchmark.setCategoryGroup(request.getCategoryGroup());
        if (request.getDescription() != null) benchmark.setDescription(request.getDescription());
        if (request.getExplanation() != null) benchmark.setExplanation(request.getExplanation());
        if (request.getSortOrder() != null) benchmark.setSortOrder(request.getSortOrder());

        LLMBenchmark updated = llmBenchmarkRepository.save(benchmark);
        return convertToResponse(updated);
    }

    @CacheEvict(value = CacheNames.LLM_BENCHMARKS, allEntries = true)
    public void deleteLLMBenchmark(BenchmarkType benchmarkType) {
        if (!llmBenchmarkRepository.existsById(benchmarkType)) {
            throw new IllegalArgumentException("LLMBenchmark not found with type: " + benchmarkType);
        }
        llmBenchmarkRepository.deleteById(benchmarkType);
    }

    private LLMBenchmarkResponse convertToResponse(LLMBenchmark benchmark) {
        return LLMBenchmarkResponse.builder()
            .benchmarkType(benchmark.getBenchmarkType())
            .displayName(benchmark.getDisplayName())
            .categoryGroup(benchmark.getCategoryGroup())
            .description(benchmark.getDescription())
            .explanation(benchmark.getExplanation())
            .sortOrder(benchmark.getSortOrder())
            .build();
    }
}
