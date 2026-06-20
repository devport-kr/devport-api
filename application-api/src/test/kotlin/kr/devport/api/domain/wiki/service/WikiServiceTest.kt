package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.repository.ProjectRepository
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class WikiServiceTest {
    @Mock
    lateinit var projectRepository: ProjectRepository

    @Mock
    lateinit var wikiSectionChunkRepository: WikiSectionChunkRepository

    @InjectMocks
    lateinit var wikiService: WikiService

    @Test
    @DisplayName("getProjectWiki builds sections from summary and body chunks")
    fun getProjectWikiBuildsSectionsFromChunks() {
        val project =
            Project().apply {
                id = 11L
                externalId = "github:repo"
                fullName = "owner/repo"
                stars = 100
                forks = 10
            }

        val summaryChunk =
            WikiSectionChunk().apply {
                projectExternalId = "github:repo"
                sectionId = "sec-1"
                subsectionId = null
                chunkType = "summary"
                content = "section one summary"
                metadata = mutableMapOf("titleKo" to "섹션 하나")
                commitSha = "abc"
            }

        val bodyChunk =
            WikiSectionChunk().apply {
                projectExternalId = "github:repo"
                sectionId = "sec-1"
                subsectionId = "sub-1-1"
                chunkType = "body"
                content = "body content one"
                metadata = mutableMapOf("titleKo" to "섹션 하나 — 세부")
                commitSha = "abc"
            }

        whenever(projectRepository.findByExternalId("github:repo")).thenReturn(Optional.of(project))
        whenever(wikiSectionChunkRepository.findByProjectExternalId("github:repo"))
            .thenReturn(listOf(summaryChunk, bodyChunk))

        val response = wikiService.getProjectWiki("github:repo")

        assertThat(response.sections).hasSize(1)
        assertThat(response.sections!![0].sectionId).isEqualTo("sec-1")
        assertThat(response.sections!![0].heading).isEqualTo("섹션 하나")
        assertThat(response.sections!![0].summary).isEqualTo("section one summary")
        assertThat(response.sections!![0].deepDiveMarkdown).isEqualTo("## 섹션 하나 — 세부\n\nbody content one")
        assertThat(response.anchors).hasSize(1)
        assertThat(response.currentCounters).isNotNull()
        assertThat(response.currentCounters!!.stars).isEqualTo(100)
    }

    @Test
    @DisplayName("getProjects uses first summary chunk for browse summary")
    fun getProjectsUsesSummaryChunk() {
        val project =
            Project().apply {
                id = 1L
                externalId = "github:repo"
                fullName = "owner/repo"
                description = "desc"
                stars = 123
                language = "Java"
            }

        val summaryChunk =
            WikiSectionChunk().apply {
                projectExternalId = "github:repo"
                sectionId = "sec-1"
                subsectionId = null
                chunkType = "summary"
                content = "browse summary text"
                commitSha = "abc"
            }

        whenever(projectRepository.findAll(any<Sort>())).thenReturn(mutableListOf(project))
        whenever(wikiSectionChunkRepository.findAllSummaryChunks()).thenReturn(listOf(summaryChunk))

        val response = wikiService.getProjects()

        assertThat(response.projects).hasSize(1)
        assertThat(response.projects!![0].summary).isEqualTo("browse summary text")
    }

    @Test
    @DisplayName("getProjects excludes projects with no chunks")
    fun getProjectsExcludesProjectsWithNoChunks() {
        val project =
            Project().apply {
                id = 9L
                externalId = "github:none"
                fullName = "owner/none"
                description = "desc"
                stars = 1
                language = "Java"
            }

        whenever(projectRepository.findAll(any<Sort>())).thenReturn(mutableListOf(project))
        whenever(wikiSectionChunkRepository.findAllSummaryChunks()).thenReturn(emptyList())

        assertThat(wikiService.getProjects().projects).isEmpty()
    }
}
