package kr.devport.api.domain.wiki.controller

import kr.devport.api.domain.wiki.dto.response.WikiAdminProjectSummaryResponse
import kr.devport.api.domain.wiki.service.WikiAdminProjectQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/wiki/admin/projects")
class WikiAdminProjectController(
    private val wikiAdminProjectQueryService: WikiAdminProjectQueryService,
) {
    @GetMapping
    fun listProjects(): ResponseEntity<List<WikiAdminProjectSummaryResponse>> =
        ResponseEntity.ok(wikiAdminProjectQueryService.listProjects())
}
