package kr.devport.api.domain.port.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.devport.api.domain.port.enums.EventType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "projects",
    indexes = [
        Index(name = "idx_projects_stars", columnList = "stars"),
        Index(name = "idx_projects_stars_week_delta", columnList = "stars_week_delta"),
    ],
)
class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "external_id", unique = true, nullable = false, length = 100)
    var externalId: String? = null

    @Column(nullable = false, length = 100)
    var name: String? = null

    @Column(name = "full_name", nullable = false, length = 200)
    var fullName: String? = null

    @Column(name = "repo_url", nullable = false, length = 500)
    var repoUrl: String? = null

    @Column(name = "homepage_url", length = 500)
    var homepageUrl: String? = null

    @Column(columnDefinition = "TEXT")
    var description: String? = null

    @Column
    var stars: Int? = 0

    @Column(name = "stars_week_delta")
    var starsWeekDelta: Int? = 0

    @Column
    var forks: Int? = 0

    @Column
    var contributors: Int? = 0

    @Column(length = 50)
    var language: String? = null

    @Column(name = "language_color", length = 7)
    var languageColor: String? = null

    @Column(length = 50)
    var license: String? = null

    @Column(name = "last_release")
    var lastRelease: LocalDate? = null

    @Column(name = "releases_30d")
    var releases30d: Int? = 0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var tags: MutableList<String>? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        if (externalId == null) {
            externalId = UUID.randomUUID().toString()
        }
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private val project = Project()

        fun id(id: Long?) = apply { project.id = id }

        fun externalId(externalId: String?) = apply { project.externalId = externalId }

        fun name(name: String?) = apply { project.name = name }

        fun fullName(fullName: String?) = apply { project.fullName = fullName }

        fun repoUrl(repoUrl: String?) = apply { project.repoUrl = repoUrl }

        fun homepageUrl(homepageUrl: String?) = apply { project.homepageUrl = homepageUrl }

        fun description(description: String?) = apply { project.description = description }

        fun stars(stars: Int?) = apply { project.stars = stars }

        fun starsWeekDelta(starsWeekDelta: Int?) = apply { project.starsWeekDelta = starsWeekDelta }

        fun forks(forks: Int?) = apply { project.forks = forks }

        fun contributors(contributors: Int?) = apply { project.contributors = contributors }

        fun language(language: String?) = apply { project.language = language }

        fun languageColor(languageColor: String?) = apply { project.languageColor = languageColor }

        fun license(license: String?) = apply { project.license = license }

        fun lastRelease(lastRelease: LocalDate?) = apply { project.lastRelease = lastRelease }

        fun releases30d(releases30d: Int?) = apply { project.releases30d = releases30d }

        fun tags(tags: MutableList<String>?) = apply { project.tags = tags }

        fun createdAt(createdAt: LocalDateTime?) = apply { project.createdAt = createdAt }

        fun updatedAt(updatedAt: LocalDateTime?) = apply { project.updatedAt = updatedAt }

        fun build(): Project = project
    }
}

@Entity
@Table(
    name = "project_comments",
    indexes = [
        Index(name = "idx_project_comments_project", columnList = "project_id"),
        Index(name = "idx_project_comments_parent", columnList = "parent_id"),
    ],
)
class ProjectComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "external_id", unique = true, nullable = false, length = 100)
    var externalId: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Project? = null

    // Cross-domain reference by id (auth owns User). Resolve via auth's UserDirectory port.
    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    var parentComment: ProjectComment? = null

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String? = null

    @Column(nullable = false)
    var deleted: Boolean = false

    @Column(name = "vote_score", nullable = false)
    var voteScore: Int = 0

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        if (externalId == null) {
            externalId = UUID.randomUUID().toString()
        }
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }
}

@Entity
@Table(
    name = "project_comment_votes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_project_comment_votes", columnNames = ["comment_id", "user_id"]),
    ],
    indexes = [
        Index(name = "idx_project_comment_votes_comment", columnList = "comment_id"),
    ],
)
class ProjectCommentVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comment_id", nullable = false)
    var comment: ProjectComment? = null

    // Cross-domain reference by id (auth owns User). Resolve via auth's UserDirectory port.
    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    @Column(nullable = false)
    var vote: Short = 0

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}

@Entity
@Table(
    name = "project_events",
    indexes = [
        Index(name = "idx_project_events_project_released", columnList = "project_id,released_at"),
        Index(name = "idx_project_events_security", columnList = "is_security"),
        Index(name = "idx_project_events_breaking", columnList = "is_breaking"),
    ],
)
class ProjectEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "external_id", unique = true, nullable = false, length = 100)
    var externalId: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Project? = null

    @Column(nullable = false, length = 50)
    var version: String? = null

    @Column(name = "released_at", nullable = false)
    var releasedAt: LocalDate? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_event_types", joinColumns = [JoinColumn(name = "event_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    var eventTypes: MutableSet<EventType> = mutableSetOf()

    @Column(nullable = false, columnDefinition = "TEXT")
    var summary: String? = null

    @ElementCollection
    @CollectionTable(name = "project_event_bullets", joinColumns = [JoinColumn(name = "event_id")])
    @Column(name = "bullet", columnDefinition = "TEXT")
    var bullets: MutableList<String> = mutableListOf()

    @Column(name = "impact_score")
    var impactScore: Int? = null

    @Column(name = "is_security")
    var isSecurity: Boolean = false

    @Column(name = "is_breaking")
    var isBreaking: Boolean = false

    @Column(name = "source_url", length = 500)
    var sourceUrl: String? = null

    @Column(name = "raw_notes", columnDefinition = "TEXT")
    var rawNotes: String? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        if (externalId == null) {
            externalId = UUID.randomUUID().toString()
        }
        createdAt = LocalDateTime.now()
    }
}

@Entity
@Table(
    name = "project_metrics_daily",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_project_metrics_daily", columnNames = ["project_id", "date"]),
    ],
)
class ProjectMetricsDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Project? = null

    @Column(nullable = false)
    var date: LocalDate? = null

    @Column
    var stars: Int? = null

    @Column
    var forks: Int? = null

    @Column(name = "open_issues")
    var openIssues: Int? = null

    @Column
    var contributors: Int? = null
}
