package kr.devport.gitrepo

import java.time.LocalDateTime

/** GitHub 트렌딩 repository 및 메타데이터 — 순수 도메인 모델 (DB/웹 기술 무관). */
data class GitRepo(
    val id: Long?,
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
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
