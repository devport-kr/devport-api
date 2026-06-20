package kr.devport.gitrepo.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.devport.gitrepo.Category
import java.time.LocalDateTime

/**
 * JPA persistence model for the `git_repos` table. Lives only in the adapter; never crosses
 * a module boundary. Column mapping is identical to the legacy entity so the existing schema
 * (Hibernate ddl-auto) is unchanged. kotlin-jpa supplies the no-arg constructor + open classes.
 */
@Entity
@Table(name = "git_repos")
class GitRepoEntity(
    @Column(nullable = false, length = 500, name = "full_name")
    var fullName: String,
    @Column(nullable = false, length = 1000, unique = true)
    var url: String,
    @Column(columnDefinition = "TEXT")
    var description: String?,
    @Column(length = 100)
    var language: String?,
    @Column
    var stars: Int?,
    @Column
    var forks: Int?,
    @Column(name = "stars_this_week")
    var starsThisWeek: Int?,
    @Column(length = 500, name = "summary_ko_title")
    var summaryKoTitle: String?,
    @Column(columnDefinition = "TEXT", name = "summary_ko_body")
    var summaryKoBody: String?,
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    var category: Category?,
    @Column(nullable = false)
    var score: Int,
    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime,
    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
