package kr.devport.api.domain.wiki.dto.response

import java.time.OffsetDateTime

/**
 * Project wiki page with dynamic sections, anchors, and current counters. Cached in Redis, so the
 * class + nested-class names are preserved exactly from the original DTO.
 */
data class WikiProjectPageResponse(
    val projectExternalId: String? = null,
    val fullName: String? = null,
    val generatedAt: OffsetDateTime? = null,
    val sections: List<WikiSection>? = null,
    val anchors: List<AnchorItem>? = null,
    val currentCounters: CurrentCounters? = null,
    val rightRail: RightRailOrdering? = null,
) {
    data class WikiSection(
        val sectionId: String? = null,
        val heading: String? = null,
        val anchor: String? = null,
        val summary: String? = null,
        val deepDiveMarkdown: String? = null,
        val defaultExpanded: Boolean = false,
        val generatedDiagramDsl: String? = null,
        val diagramMetadata: DiagramMetadata? = null,
        val metadata: Map<String, Any>? = null,
    )

    data class AnchorItem(
        val sectionId: String? = null,
        val heading: String? = null,
        val anchor: String? = null,
    )

    data class DiagramMetadata(
        val diagramType: String? = null,
        val altText: String? = null,
        val renderHints: String? = null,
    )

    data class CurrentCounters(
        val stars: Int? = null,
        val forks: Int? = null,
        val watchers: Int? = null,
        val openIssues: Int? = null,
        val updatedAt: OffsetDateTime? = null,
    )

    data class RightRailOrdering(
        val activityPriority: Int = 0,
        val releasesPriority: Int = 0,
        val chatPriority: Int = 0,
        val visibleSectionIds: List<String>? = null,
    )
}
