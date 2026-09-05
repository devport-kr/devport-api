package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.common.cache.CacheNames
import kr.devport.api.domain.port.infrastructure.ProjectDirectory
import kr.devport.api.domain.wiki.dto.response.WikiProjectListResponse
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.infrastructure.WikiSectionChunkRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.regex.Pattern

/**
 * Wiki read orchestration. Serves wiki pages directly from wiki_section_chunks.
 */
@Service
@Transactional(readOnly = true)
class WikiService(
    private val projectDirectory: ProjectDirectory,
    private val wikiSectionChunkRepository: WikiSectionChunkRepository,
) {
    @Cacheable(cacheNames = [CacheNames.WIKI_PROJECTS])
    fun getProjects(): WikiProjectListResponse {
        val projects = projectDirectory.listAllByStarsDesc()

        // Single query for all summary chunks across all projects (avoids N+1); keep first on dupes.
        val summaryByProject =
            buildMap {
                wikiSectionChunkRepository.findAllSummaryChunks().forEach { chunk ->
                    val pid = chunk.projectExternalId ?: return@forEach
                    putIfAbsent(pid, chunk.content ?: "")
                }
            }

        val projectSummaries =
            projects.mapNotNull { project ->
                val summary = project.externalId?.let { summaryByProject[it] }.orEmpty()
                if (summary.isBlank()) {
                    null
                } else {
                    WikiProjectListResponse.ProjectSummary(
                        projectExternalId = project.externalId,
                        fullName = project.fullName,
                        description = project.description,
                        stars = project.stars,
                        language = project.language,
                        summary = summary,
                    )
                }
            }

        return WikiProjectListResponse(projects = projectSummaries)
    }

    @Cacheable(cacheNames = [CacheNames.WIKI_PROJECT_PAGE], key = "#projectExternalId")
    fun getProjectWiki(projectExternalId: String): WikiProjectPageResponse {
        val project =
            projectDirectory.findByExternalId(projectExternalId)
                ?: throw IllegalArgumentException("Project not found: $projectExternalId")

        val chunks = wikiSectionChunkRepository.findByProjectExternalId(projectExternalId)
        if (chunks.isEmpty()) {
            throw IllegalArgumentException("No wiki content found for project: $projectExternalId")
        }

        val bySectionId = chunks.groupBy { it.sectionId }
        val sortedSectionIds = bySectionId.keys.sortedBy { extractTrailingNumber(it) }

        val generatedAt: OffsetDateTime? = chunks.mapNotNull { it.updatedAt }.maxOrNull()

        val sections = sortedSectionIds.map { sectionId -> buildSection(sectionId, bySectionId.getValue(sectionId)) }

        val anchors =
            sections.map { section ->
                WikiProjectPageResponse.AnchorItem(
                    sectionId = section.sectionId,
                    heading = section.heading,
                    anchor = section.anchor,
                )
            }

        val rightRail =
            WikiProjectPageResponse.RightRailOrdering(
                activityPriority = 1,
                releasesPriority = 2,
                chatPriority = 3,
                visibleSectionIds = anchors.mapNotNull { it.sectionId },
            )

        val currentCounters =
            WikiProjectPageResponse.CurrentCounters(
                stars = project.stars,
                forks = project.forks,
            )

        return WikiProjectPageResponse(
            projectExternalId = projectExternalId,
            fullName = project.fullName,
            generatedAt = generatedAt,
            sections = sections,
            anchors = anchors,
            currentCounters = currentCounters,
            rightRail = rightRail,
        )
    }

    private fun buildSection(
        sectionId: String?,
        sectionChunks: List<WikiSectionChunk>,
    ): WikiProjectPageResponse.WikiSection {
        val summaryChunk = sectionChunks.firstOrNull { it.chunkType == "summary" && it.subsectionId == null }

        val summary = summaryChunk?.content ?: ""
        var heading: String? = sectionId

        summaryChunk?.metadata?.get("titleKo")?.let { titleKo ->
            if (titleKo.toString().isNotBlank()) {
                heading = titleKo.toString()
            }
        }

        if (heading == sectionId) {
            val overviewChunk = sectionChunks.firstOrNull { it.chunkType == "overview" }
            overviewChunk?.metadata?.get("titleKo")?.let { titleKo ->
                if (titleKo.toString().isNotBlank()) {
                    heading = titleKo.toString()
                }
            }
        }

        val deepDiveMarkdown =
            sectionChunks
                .filter { it.chunkType == "body" || it.chunkType == "overview" }
                .sortedBy { extractTrailingNumber(it.subsectionId) }
                .joinToString("\n\n") { c ->
                    if (c.chunkType == "overview") {
                        c.content ?: ""
                    } else {
                        val titleKo = c.metadata?.get("titleKo")?.toString()?.takeUnless { it.isBlank() }
                        if (titleKo != null) "## $titleKo\n\n${c.content}" else (c.content ?: "")
                    }
                }

        return WikiProjectPageResponse.WikiSection(
            sectionId = sectionId,
            heading = heading,
            anchor = sectionId,
            summary = summary,
            deepDiveMarkdown = deepDiveMarkdown,
        )
    }

    private fun extractTrailingNumber(id: String?): Int {
        if (id == null) {
            return Int.MAX_VALUE
        }
        val m = NUMERIC_SUFFIX.matcher(id)
        return if (m.find()) m.group().toInt() else Int.MAX_VALUE
    }

    companion object {
        private val NUMERIC_SUFFIX: Pattern = Pattern.compile("\\d+$")
    }
}
