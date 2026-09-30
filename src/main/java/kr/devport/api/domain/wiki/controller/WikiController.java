package kr.devport.api.domain.wiki.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.wiki.dto.response.WikiProjectListResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.service.WikiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public wiki browse and project detail endpoints.
 * Returns dynamic section payloads in summary-first progressive structure.
 * Enforces data-ready-only listing and hide-incomplete section behavior.
 */
@Tag(name = "Wiki", description = "Public wiki endpoints for domain browsing and project pages")
@RestController
@RequestMapping("/api/wiki")
@RequiredArgsConstructor
public class WikiController {

    private final WikiService wikiService;

    @Operation(
            summary = "List all wiki-ready projects",
            description = "Returns all projects that have wiki content, sorted by stars descending."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Project list retrieved successfully",
                    content = @Content(schema = @Schema(implementation = WikiProjectListResponse.class))
            )
    })
    @GetMapping("/projects")
    public ResponseEntity<WikiProjectListResponse> listProjects() {
        WikiProjectListResponse response = wikiService.getProjects();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get project wiki page",
            description = "Returns project wiki with dynamic sections and generated anchors. " +
                    "Incomplete/uncertain sections are omitted and metrics are exposed as current counters only."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Project wiki page retrieved successfully",
                    content = @Content(schema = @Schema(implementation = WikiProjectPageResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Project not found or wiki not ready", content = @Content)
    })
    @GetMapping("/projects/{projectExternalId}")
    public ResponseEntity<WikiProjectPageResponse> getProjectWiki(@PathVariable String projectExternalId) {
        WikiProjectPageResponse response = wikiService.getProjectWiki(projectExternalId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get project wiki page by query param",
            description = "Same as /projects/{id} but accepts id as a query param to avoid encoded-slash issues"
    )
    @GetMapping("/projects/page")
    public ResponseEntity<WikiProjectPageResponse> getProjectWikiByQueryId(@RequestParam String id) {
        WikiProjectPageResponse response = wikiService.getProjectWiki(id);
        return ResponseEntity.ok(response);
    }
}

