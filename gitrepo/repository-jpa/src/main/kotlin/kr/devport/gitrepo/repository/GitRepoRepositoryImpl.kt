package kr.devport.gitrepo.repository

import kr.devport.gitrepo.Category
import kr.devport.gitrepo.GitRepo
import kr.devport.gitrepo.GitRepoPage
import kr.devport.gitrepo.infrastructure.GitRepoRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/** Out-Port implementation. Translates between Spring Data paging/entities and domain models. */
class GitRepoRepositoryImpl(
    private val jpa: GitRepoJpaRepository,
) : GitRepoRepository {
    override fun findPage(
        category: Category?,
        page: Int,
        size: Int,
    ): GitRepoPage {
        val pageable =
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "score").and(Sort.by(Sort.Direction.DESC, "createdAt")),
            )
        val result = if (category == null) jpa.findAll(pageable) else jpa.findByCategory(category, pageable)
        return result.toGitRepoPage()
    }

    override fun findTrending(
        page: Int,
        size: Int,
    ): GitRepoPage = jpa.findAllByOrderByStarsThisWeekDesc(PageRequest.of(page, size)).toGitRepoPage()

    override fun findByLanguage(
        language: String,
        limit: Int,
    ): List<GitRepo> = jpa.findByLanguageOrderByScoreDesc(language, PageRequest.of(0, limit)).content.map { it.toModel() }

    override fun findById(id: Long): GitRepo? = jpa.findById(id).map { it.toModel() }.orElse(null)

    override fun save(gitRepo: GitRepo): GitRepo = jpa.save(gitRepo.toEntity()).toModel()

    override fun existsById(id: Long): Boolean = jpa.existsById(id)

    override fun deleteById(id: Long) = jpa.deleteById(id)
}

private fun Page<GitRepoEntity>.toGitRepoPage(): GitRepoPage =
    GitRepoPage(
        content = content.map { it.toModel() },
        totalElements = totalElements,
        totalPages = totalPages,
        currentPage = number,
        hasMore = hasNext(),
    )

private fun GitRepoEntity.toModel(): GitRepo =
    GitRepo(
        id = id,
        fullName = fullName,
        url = url,
        description = description,
        language = language,
        stars = stars,
        forks = forks,
        starsThisWeek = starsThisWeek,
        summaryKoTitle = summaryKoTitle,
        summaryKoBody = summaryKoBody,
        category = category,
        score = score,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun GitRepo.toEntity(): GitRepoEntity =
    GitRepoEntity(
        fullName = fullName,
        url = url,
        description = description,
        language = language,
        stars = stars,
        forks = forks,
        starsThisWeek = starsThisWeek,
        summaryKoTitle = summaryKoTitle,
        summaryKoBody = summaryKoBody,
        category = category,
        score = score,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ).also { it.id = this.id }
