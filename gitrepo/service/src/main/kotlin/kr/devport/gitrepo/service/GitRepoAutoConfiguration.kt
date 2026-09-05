package kr.devport.gitrepo.service

import kr.devport.gitrepo.infrastructure.GitRepoRepository
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

/** Wires the in-port implementations explicitly (no @ComponentScan). */
@AutoConfiguration
class GitRepoAutoConfiguration {
    @Bean
    fun gitRepoService(gitRepoRepository: GitRepoRepository): GitRepoService = GitRepoServiceImpl(gitRepoRepository)

    @Bean
    fun gitRepoAdminService(gitRepoRepository: GitRepoRepository): GitRepoAdminService = GitRepoAdminServiceImpl(gitRepoRepository)
}
