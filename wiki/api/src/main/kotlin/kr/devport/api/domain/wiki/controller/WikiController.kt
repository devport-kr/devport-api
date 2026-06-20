package kr.devport.api.domain.wiki.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.devport.api.domain.wiki.dto.response.WikiProjectListResponse
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse
import kr.devport.api.domain.wiki.service.WikiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Public wiki browse + project detail endpoints. Returns summary-first progressive section payloads;
 * incomplete sections are omitted and metrics are exposed as current counters only.
 */
@Tag(name = "Wiki", description = "Public wiki endpoints for domain browsing and project pages")
@RestController
@RequestMapping("/api/wiki")
class WikiController(
    private val wikiService: WikiService,
) {
    @Operation(summary = "List all wiki-ready projects (sorted by stars desc)")
    @GetMapping("/projects")
    fun listProjects(): ResponseEntity<WikiProjectListResponse> = ResponseEntity.ok(wikiService.getProjects())

    @Operation(summary = "Get project wiki page with dynamic sections and anchors")
    @GetMapping("/projects/{projectExternalId}")
    fun getProjectWiki(
        @PathVariable projectExternalId: String,
    ): ResponseEntity<WikiProjectPageResponse> = ResponseEntity.ok(wikiService.getProjectWiki(projectExternalId))

    @Operation(summary = "Get project wiki page by query param (avoids encoded-slash issues)")
    @GetMapping("/projects/page")
    fun getProjectWikiByQueryId(
        @RequestParam id: String,
    ): ResponseEntity<WikiProjectPageResponse> = ResponseEntity.ok(wikiService.getProjectWiki(id))
}
