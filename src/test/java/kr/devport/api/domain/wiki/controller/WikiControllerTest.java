package kr.devport.api.domain.wiki.controller;

import kr.devport.api.domain.wiki.dto.response.WikiProjectListResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.service.WikiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiControllerTest {

    @Mock
    private WikiService wikiService;

    @InjectMocks
    private WikiController wikiController;

    @Test
    @DisplayName("listProjects returns flat project list from service")
    void listProjects_returnsServicePayload() {
        WikiProjectListResponse payload = WikiProjectListResponse.builder()
                .projects(List.of())
                .build();
        when(wikiService.getProjects()).thenReturn(payload);

        ResponseEntity<WikiProjectListResponse> response = wikiController.listProjects();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(payload);
    }

    @Test
    @DisplayName("getProjectWiki returns dynamic section response")
    void getProjectWiki_returnsServicePayload() {
        WikiProjectPageResponse payload = WikiProjectPageResponse.builder()
                .projectExternalId("github:repo")
                .fullName("owner/repo")
                .sections(List.of())
                .anchors(List.of())
                .build();
        when(wikiService.getProjectWiki("github:repo")).thenReturn(payload);

        ResponseEntity<WikiProjectPageResponse> response = wikiController.getProjectWiki("github:repo");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(payload);
    }
}
