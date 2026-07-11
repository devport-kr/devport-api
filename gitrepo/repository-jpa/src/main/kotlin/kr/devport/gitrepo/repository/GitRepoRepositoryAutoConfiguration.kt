package kr.devport.gitrepo.repository

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

/**
 * Registers the out-port adapter. The GitRepoJpaRepository proxy + GitRepoEntity are picked up
 * by the composition root's @EnableJpaRepositories / @EntityScan (which list this package).
 */
@AutoConfiguration
class GitRepoRepositoryAutoConfiguration {
    @Bean
    fun gitRepoRepository(gitRepoJpaRepository: GitRepoJpaRepository): GitRepoRepositoryImpl = GitRepoRepositoryImpl(gitRepoJpaRepository)
}
