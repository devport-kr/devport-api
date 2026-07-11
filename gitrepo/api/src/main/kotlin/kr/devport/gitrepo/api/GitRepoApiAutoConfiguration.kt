package kr.devport.gitrepo.api

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

/** Registers the gitrepo REST controllers. */
@AutoConfiguration
@Import(GitRepoController::class, GitRepoAdminController::class)
class GitRepoApiAutoConfiguration
