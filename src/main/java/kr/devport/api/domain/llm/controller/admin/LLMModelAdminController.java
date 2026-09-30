package kr.devport.api.domain.llm.controller.admin;

import jakarta.validation.Valid;
import kr.devport.api.domain.llm.dto.request.admin.LLMModelCreateRequest;
import kr.devport.api.domain.llm.dto.request.admin.LLMModelUpdateRequest;
import kr.devport.api.domain.llm.dto.response.LLMModelDetailResponse;
import kr.devport.api.domain.llm.service.admin.LLMModelAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/llm-models")
@RequiredArgsConstructor
public class LLMModelAdminController {

    private final LLMModelAdminService llmModelAdminService;

    @PostMapping
    public ResponseEntity<LLMModelDetailResponse> createLLMModel(@Valid @RequestBody LLMModelCreateRequest request) {
        LLMModelDetailResponse response = llmModelAdminService.createLLMModel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LLMModelDetailResponse> updateLLMModel(
        @PathVariable Long id,
        @Valid @RequestBody LLMModelUpdateRequest request
    ) {
        LLMModelDetailResponse response = llmModelAdminService.updateLLMModel(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLLMModel(@PathVariable Long id) {
        llmModelAdminService.deleteLLMModel(id);
        return ResponseEntity.noContent().build();
    }
}
