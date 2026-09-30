package kr.devport.api.domain.port.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.port.dto.response.ProjectCommentResponse;
import kr.devport.api.domain.port.dto.response.ProjectDetailResponse;
import kr.devport.api.domain.port.dto.response.ProjectEventResponse;
import kr.devport.api.domain.port.enums.EventType;
import kr.devport.api.domain.port.service.ProjectCommentService;
import kr.devport.api.domain.port.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;



@Tag(name = "Projects", description = "Project endpoints")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectCommentService projectCommentService;

    @Operation(
        summary = "Get project by ID",
        description = "Retrieve full project details"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Project retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProjectDetailResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailResponse> getProject(@PathVariable String id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @Operation(
        summary = "Get project by query param ID",
        description = "Same as /{id} but accepts id as a query param to avoid encoded-slash issues"
    )
    @GetMapping
    public ResponseEntity<ProjectDetailResponse> getProjectByQueryId(@RequestParam String id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @Operation(
        summary = "Get project events",
        description = "Retrieve project release timeline with optional event type filtering"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Events retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProjectEventResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @GetMapping("/{id}/events")
    public ResponseEntity<Page<ProjectEventResponse>> getEvents(
        @PathVariable String id,
        @RequestParam(required = false) EventType type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("releasedAt").descending());
        return ResponseEntity.ok(projectService.getProjectEvents(id, type, pageable));
    }

    @Operation(
        summary = "Get project events by query param ID",
        description = "Same as /{id}/events but accepts id as a query param to avoid encoded-slash issues"
    )
    @GetMapping("/events")
    public ResponseEntity<Page<ProjectEventResponse>> getEventsByQueryId(
        @RequestParam String id,
        @RequestParam(required = false) EventType type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("releasedAt").descending());
        return ResponseEntity.ok(projectService.getProjectEvents(id, type, pageable));
    }

    @Operation(
        summary = "Get project comments by query param",
        description = "Accepts projectId as a query param to avoid encoded-slash issues in path variables"
    )
    @GetMapping("/comments")
    public ResponseEntity<List<ProjectCommentResponse>> getCommentsByQueryId(
        @RequestParam String projectId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(projectCommentService.getCommentsByProject(projectId, userId));
    }

}
