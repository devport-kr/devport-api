package kr.devport.api.domain.wiki.controller;

import kr.devport.api.domain.wiki.dto.response.WikiAdminProjectSummaryResponse;
import kr.devport.api.domain.wiki.service.WikiAdminProjectQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wiki/admin/projects")
@RequiredArgsConstructor
public class WikiAdminProjectController {

    private final WikiAdminProjectQueryService wikiAdminProjectQueryService;

    @GetMapping
    public ResponseEntity<List<WikiAdminProjectSummaryResponse>> listProjects() {
        return ResponseEntity.ok(wikiAdminProjectQueryService.listProjects());
    }
}
