package kr.devport.api.domain.llm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.llm.dto.response.ImageEditingModelResponse;
import kr.devport.api.domain.llm.dto.response.ImageToVideoModelResponse;
import kr.devport.api.domain.llm.dto.response.TextToImageModelResponse;
import kr.devport.api.domain.llm.dto.response.TextToSpeechModelResponse;
import kr.devport.api.domain.llm.dto.response.TextToVideoModelResponse;
import kr.devport.api.domain.llm.service.LLMMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LLM Media", description = "Media model benchmarks from Artificial Analysis (public)")
@RestController
@RequestMapping("/api/llm/media")
@RequiredArgsConstructor
public class LLMMediaController {

    private final LLMMediaService llmMediaService;

    @Operation(
        summary = "Get text-to-image models",
        description = "Retrieve paginated text-to-image model rankings."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved text-to-image models",
            content = @Content(schema = @Schema(implementation = Page.class))
        )
    })
    @GetMapping("/text-to-image")
    public ResponseEntity<Page<TextToImageModelResponse>> getTextToImageModels(
        @PageableDefault(size = 20, sort = "rank", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<TextToImageModelResponse> models = llmMediaService.getTextToImageModels(pageable);
        return ResponseEntity.ok(models);
    }

    @Operation(
        summary = "Get image editing models",
        description = "Retrieve paginated image editing model rankings."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved image editing models",
            content = @Content(schema = @Schema(implementation = Page.class))
        )
    })
    @GetMapping("/image-editing")
    public ResponseEntity<Page<ImageEditingModelResponse>> getImageEditingModels(
        @PageableDefault(size = 20, sort = "rank", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ImageEditingModelResponse> models = llmMediaService.getImageEditingModels(pageable);
        return ResponseEntity.ok(models);
    }

    @Operation(
        summary = "Get text-to-speech models",
        description = "Retrieve paginated text-to-speech model rankings."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved text-to-speech models",
            content = @Content(schema = @Schema(implementation = Page.class))
        )
    })
    @GetMapping("/text-to-speech")
    public ResponseEntity<Page<TextToSpeechModelResponse>> getTextToSpeechModels(
        @PageableDefault(size = 20, sort = "rank", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<TextToSpeechModelResponse> models = llmMediaService.getTextToSpeechModels(pageable);
        return ResponseEntity.ok(models);
    }

    @Operation(
        summary = "Get text-to-video models",
        description = "Retrieve paginated text-to-video model rankings."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved text-to-video models",
            content = @Content(schema = @Schema(implementation = Page.class))
        )
    })
    @GetMapping("/text-to-video")
    public ResponseEntity<Page<TextToVideoModelResponse>> getTextToVideoModels(
        @PageableDefault(size = 20, sort = "rank", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<TextToVideoModelResponse> models = llmMediaService.getTextToVideoModels(pageable);
        return ResponseEntity.ok(models);
    }

    @Operation(
        summary = "Get image-to-video models",
        description = "Retrieve paginated image-to-video model rankings."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved image-to-video models",
            content = @Content(schema = @Schema(implementation = Page.class))
        )
    })
    @GetMapping("/image-to-video")
    public ResponseEntity<Page<ImageToVideoModelResponse>> getImageToVideoModels(
        @PageableDefault(size = 20, sort = "rank", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ImageToVideoModelResponse> models = llmMediaService.getImageToVideoModels(pageable);
        return ResponseEntity.ok(models);
    }
}
