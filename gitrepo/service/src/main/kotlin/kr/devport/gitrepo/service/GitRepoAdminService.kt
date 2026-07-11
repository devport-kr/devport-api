package kr.devport.gitrepo.service

import kr.devport.api.domain.common.cache.CacheNames
import kr.devport.gitrepo.GitRepo
import kr.devport.gitrepo.infrastructure.GitRepoRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** In-Port: administrative write use-cases. */
interface GitRepoAdminService {
    fun create(command: GitRepoCreateCommand): GitRepo

    fun update(
        id: Long,
        command: GitRepoUpdateCommand,
    ): GitRepo

    fun delete(id: Long)
}

@Transactional
internal class GitRepoAdminServiceImpl(
    private val gitRepoRepository: GitRepoRepository,
) : GitRepoAdminService {
    @CacheEvict(
        cacheNames = [CacheNames.GIT_REPOS, CacheNames.TRENDING_GIT_REPOS, CacheNames.GIT_REPOS_BY_LANGUAGE],
        allEntries = true,
    )
    override fun create(command: GitRepoCreateCommand): GitRepo {
        val now = LocalDateTime.now()
        return gitRepoRepository.save(
            GitRepo(
                id = null,
                fullName = command.fullName,
                url = command.url,
                description = command.description,
                language = command.language,
                stars = command.stars,
                forks = command.forks,
                starsThisWeek = command.starsThisWeek,
                summaryKoTitle = command.summaryKoTitle,
                summaryKoBody = command.summaryKoBody,
                category = command.category,
                score = command.score,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    @CacheEvict(
        cacheNames = [CacheNames.GIT_REPOS, CacheNames.TRENDING_GIT_REPOS, CacheNames.GIT_REPOS_BY_LANGUAGE],
        allEntries = true,
    )
    override fun update(
        id: Long,
        command: GitRepoUpdateCommand,
    ): GitRepo {
        val existing =
            gitRepoRepository.findById(id)
                ?: throw IllegalArgumentException("GitRepo not found with id: $id")
        val updated =
            existing.copy(
                fullName = command.fullName ?: existing.fullName,
                url = command.url ?: existing.url,
                description = command.description ?: existing.description,
                language = command.language ?: existing.language,
                stars = command.stars ?: existing.stars,
                forks = command.forks ?: existing.forks,
                starsThisWeek = command.starsThisWeek ?: existing.starsThisWeek,
                summaryKoTitle = command.summaryKoTitle ?: existing.summaryKoTitle,
                summaryKoBody = command.summaryKoBody ?: existing.summaryKoBody,
                category = command.category ?: existing.category,
                score = command.score ?: existing.score,
                updatedAt = LocalDateTime.now(),
            )
        return gitRepoRepository.save(updated)
    }

    @CacheEvict(
        cacheNames = [CacheNames.GIT_REPOS, CacheNames.TRENDING_GIT_REPOS, CacheNames.GIT_REPOS_BY_LANGUAGE],
        allEntries = true,
    )
    override fun delete(id: Long) {
        if (!gitRepoRepository.existsById(id)) {
            throw IllegalArgumentException("GitRepo not found with id: $id")
        }
        gitRepoRepository.deleteById(id)
    }
}
