package kr.devport.api.domain.wiki.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.wiki.dto.response.WikiDomainBrowseResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.service.WikiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
            summary = "Browse wiki projects by domain",
            description = "Returns top-starred data-ready projects for a given domain (port slug). " +
                    "Only projects with complete wiki snapshots are included."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Domain browse listing retrieved successfully",
                    content = @Content(schema = @Schema(implementation = WikiDomainBrowseResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Domain not found", content = @Content)
    })
    @GetMapping("/domains/{domain}")
    public ResponseEntity<WikiDomainBrowseResponse> browseByDomain(@PathVariable String domain) {
        WikiDomainBrowseResponse response = wikiService.getProjectsByDomain(domain);
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
}
