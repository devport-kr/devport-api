package kr.devport.gitrepo.repository

import kr.devport.gitrepo.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface GitRepoJpaRepository : JpaRepository<GitRepoEntity, Long> {
    fun findByCategory(
        category: Category,
        pageable: Pageable,
    ): Page<GitRepoEntity>

    fun findAllByOrderByStarsThisWeekDesc(pageable: Pageable): Page<GitRepoEntity>

    fun findByLanguageOrderByScoreDesc(
        language: String,
        pageable: Pageable,
    ): Page<GitRepoEntity>
}
