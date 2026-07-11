package kr.devport.gitrepo.infrastructure

import kr.devport.gitrepo.Category
import kr.devport.gitrepo.GitRepo
import kr.devport.gitrepo.GitRepoPage

/**
 * Out-Port: the data access contract owned by the core. Returns domain models only —
 * the JPA adapter in :gitrepo:repository-jpa implements this and hides all persistence detail.
 */
interface GitRepoRepository {
    fun findPage(
        category: Category?,
        page: Int,
        size: Int,
    ): GitRepoPage

    fun findTrending(
        page: Int,
        size: Int,
    ): GitRepoPage

    fun findByLanguage(
        language: String,
        limit: Int,
    ): List<GitRepo>

    fun findById(id: Long): GitRepo?

    fun save(gitRepo: GitRepo): GitRepo

    fun existsById(id: Long): Boolean

    fun deleteById(id: Long)
}
