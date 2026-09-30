package kr.devport.api.domain.llm.controller.admin;

import jakarta.validation.Valid;
import kr.devport.api.domain.llm.dto.request.admin.ModelCreatorCreateRequest;
import kr.devport.api.domain.llm.dto.request.admin.ModelCreatorUpdateRequest;
import kr.devport.api.domain.llm.dto.response.ModelCreatorResponse;
import kr.devport.api.domain.llm.service.admin.ModelCreatorAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/model-creators")
@RequiredArgsConstructor
public class ModelCreatorAdminController {

    private final ModelCreatorAdminService modelCreatorAdminService;

    @PostMapping
    public ResponseEntity<ModelCreatorResponse> createModelCreator(@Valid @RequestBody ModelCreatorCreateRequest request) {
        ModelCreatorResponse response = modelCreatorAdminService.createModelCreator(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModelCreatorResponse> updateModelCreator(
        @PathVariable Long id,
        @Valid @RequestBody ModelCreatorUpdateRequest request
    ) {
        ModelCreatorResponse response = modelCreatorAdminService.updateModelCreator(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModelCreator(@PathVariable Long id) {
        modelCreatorAdminService.deleteModelCreator(id);
        return ResponseEntity.noContent().build();
    }
}
