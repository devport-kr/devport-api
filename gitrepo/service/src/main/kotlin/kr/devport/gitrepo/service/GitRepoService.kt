package kr.devport.gitrepo.service

import kr.devport.api.domain.common.cache.CacheNames
import kr.devport.gitrepo.Category
import kr.devport.gitrepo.GitRepo
import kr.devport.gitrepo.GitRepoPage
import kr.devport.gitrepo.infrastructure.GitRepoRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.transaction.annotation.Transactional

/** In-Port: read use-cases for GitHub repositories. */
interface GitRepoService {
    fun getGitRepos(
        category: Category?,
        page: Int,
        size: Int,
    ): GitRepoPage

    fun getTrendingGitRepos(
        page: Int,
        size: Int,
    ): GitRepoPage

    fun getGitReposByLanguage(
        language: String,
        limit: Int,
    ): List<GitRepo>
}

@Transactional(readOnly = true)
internal class GitRepoServiceImpl(
    private val gitRepoRepository: GitRepoRepository,
) : GitRepoService {
    @Cacheable(
        cacheNames = [CacheNames.GIT_REPOS],
        key = "@cacheKeyFactory.gitRepoListKey(#category?.name(), #page, #size)",
        unless = "@cacheFallbackBypass.shouldBypass('GIT_REPO')",
    )
    override fun getGitRepos(
        category: Category?,
        page: Int,
        size: Int,
    ): GitRepoPage = gitRepoRepository.findPage(category, page, size)

    @Cacheable(
        cacheNames = [CacheNames.TRENDING_GIT_REPOS],
        key = "@cacheKeyFactory.trendingGitReposKey(#page, #size)",
        unless = "@cacheFallbackBypass.shouldBypass('GIT_REPO')",
    )
    override fun getTrendingGitRepos(
        page: Int,
        size: Int,
    ): GitRepoPage = gitRepoRepository.findTrending(page, size)

    @Cacheable(
        cacheNames = [CacheNames.GIT_REPOS_BY_LANGUAGE],
        key = "@cacheKeyFactory.gitReposByLanguageKey(#language, #limit)",
        unless = "@cacheFallbackBypass.shouldBypass('GIT_REPO')",
    )
    override fun getGitReposByLanguage(
        language: String,
        limit: Int,
    ): List<GitRepo> = gitRepoRepository.findByLanguage(language, limit)
}
