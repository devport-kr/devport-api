package kr.devport.api.domain.wiki.controller;

import kr.devport.api.domain.wiki.dto.response.WikiDomainBrowseResponse;
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
    @DisplayName("browseByDomain returns wiki browse response from service")
    void browseByDomain_returnsServicePayload() {
        WikiDomainBrowseResponse payload = WikiDomainBrowseResponse.builder()
                .domain("web")
                .projects(List.of())
                .build();
        when(wikiService.getProjectsByDomain("web")).thenReturn(payload);

        ResponseEntity<WikiDomainBrowseResponse> response = wikiController.browseByDomain("web");

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
