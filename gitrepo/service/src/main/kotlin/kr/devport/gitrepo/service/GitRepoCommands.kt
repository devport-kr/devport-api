package kr.devport.gitrepo.service

import kr.devport.gitrepo.Category

/** In-Port input contract for creating a repo. Owned by the use-case, not by the web layer. */
data class GitRepoCreateCommand(
    val fullName: String,
    val url: String,
    val description: String?,
    val language: String?,
    val stars: Int?,
    val forks: Int?,
    val starsThisWeek: Int?,
    val summaryKoTitle: String?,
    val summaryKoBody: String?,
    val category: Category?,
    val score: Int,
)

/** Partial update: a null field means "leave unchanged". */
data class GitRepoUpdateCommand(
    val fullName: String?,
    val url: String?,
    val description: String?,
    val language: String?,
    val stars: Int?,
    val forks: Int?,
    val starsThisWeek: Int?,
    val summaryKoTitle: String?,
    val summaryKoBody: String?,
    val category: Category?,
    val score: Int?,
)
