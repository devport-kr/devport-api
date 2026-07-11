package kr.devport.api.domain.wiki.controller

import kr.devport.api.domain.wiki.dto.response.WikiProjectListResponse
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse
import kr.devport.api.domain.wiki.service.WikiService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class WikiControllerTest {
    @Mock
    lateinit var wikiService: WikiService

    @InjectMocks
    lateinit var wikiController: WikiController

    @Test
    @DisplayName("listProjects returns flat project list from service")
    fun listProjectsReturnsServicePayload() {
        val payload = WikiProjectListResponse(projects = emptyList())
        whenever(wikiService.getProjects()).thenReturn(payload)

        val response = wikiController.listProjects()

        assertThat(response.statusCode.is2xxSuccessful()).isTrue()
        assertThat(response.body).isEqualTo(payload)
    }

    @Test
    @DisplayName("getProjectWiki returns dynamic section response")
    fun getProjectWikiReturnsServicePayload() {
        val payload =
            WikiProjectPageResponse(
                projectExternalId = "github:repo",
                fullName = "owner/repo",
                sections = emptyList(),
                anchors = emptyList(),
            )
        whenever(wikiService.getProjectWiki("github:repo")).thenReturn(payload)

        val response = wikiController.getProjectWiki("github:repo")

        assertThat(response.statusCode.is2xxSuccessful()).isTrue()
        assertThat(response.body).isEqualTo(payload)
    }
}
