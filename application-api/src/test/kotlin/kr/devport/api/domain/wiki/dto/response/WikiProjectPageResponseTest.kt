package kr.devport.api.domain.wiki.dto.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class WikiProjectPageResponseTest {
    @Test
    @DisplayName("project page response supports dynamic sections anchors and current counters")
    fun supportsDynamicResponseShape() {
        val response =
            WikiProjectPageResponse(
                projectExternalId = "github:repo",
                fullName = "owner/repo",
                generatedAt = OffsetDateTime.now(),
                sections =
                    listOf(
                        WikiProjectPageResponse.WikiSection(
                            sectionId = "architecture",
                            heading = "Architecture",
                            anchor = "architecture",
                            summary = "summary",
                            deepDiveMarkdown = "deep",
                            defaultExpanded = false,
                            metadata = mapOf("category" to "core"),
                        ),
                    ),
                anchors =
                    listOf(
                        WikiProjectPageResponse.AnchorItem(
                            sectionId = "architecture",
                            heading = "Architecture",
                            anchor = "architecture",
                        ),
                    ),
                currentCounters =
                    WikiProjectPageResponse.CurrentCounters(
                        stars = 100,
                        forks = 20,
                        watchers = 5,
                        openIssues = 2,
                        updatedAt = OffsetDateTime.now(),
                    ),
            )

        assertThat(response.sections).hasSize(1)
        assertThat(response.anchors).hasSize(1)
        assertThat(response.anchors!![0].sectionId).isEqualTo("architecture")
        assertThat(response.currentCounters!!.stars).isEqualTo(100)
    }
}
