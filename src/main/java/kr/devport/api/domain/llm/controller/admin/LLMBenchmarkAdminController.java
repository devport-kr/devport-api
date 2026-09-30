package kr.devport.api.domain.llm.controller.admin;

import jakarta.validation.Valid;
import kr.devport.api.domain.llm.enums.BenchmarkType;
import kr.devport.api.domain.llm.dto.request.admin.LLMBenchmarkCreateRequest;
import kr.devport.api.domain.llm.dto.request.admin.LLMBenchmarkUpdateRequest;
import kr.devport.api.domain.llm.dto.response.LLMBenchmarkResponse;
import kr.devport.api.domain.llm.service.admin.LLMBenchmarkAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/llm-benchmarks")
@RequiredArgsConstructor
public class LLMBenchmarkAdminController {

    private final LLMBenchmarkAdminService llmBenchmarkAdminService;

    @PostMapping
    public ResponseEntity<LLMBenchmarkResponse> createLLMBenchmark(@Valid @RequestBody LLMBenchmarkCreateRequest request) {
        LLMBenchmarkResponse response = llmBenchmarkAdminService.createLLMBenchmark(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{benchmarkType}")
    public ResponseEntity<LLMBenchmarkResponse> updateLLMBenchmark(
        @PathVariable BenchmarkType benchmarkType,
        @Valid @RequestBody LLMBenchmarkUpdateRequest request
    ) {
        LLMBenchmarkResponse response = llmBenchmarkAdminService.updateLLMBenchmark(benchmarkType, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{benchmarkType}")
    public ResponseEntity<Void> deleteLLMBenchmark(@PathVariable BenchmarkType benchmarkType) {
        llmBenchmarkAdminService.deleteLLMBenchmark(benchmarkType);
        return ResponseEntity.noContent().build();
    }
}
