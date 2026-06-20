package kr.devport.gitrepo

/** 페이지네이션된 도메인 결과. 페이징 메타데이터를 코어 어휘로 표현한다. */
data class GitRepoPage(
    val content: List<GitRepo>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val hasMore: Boolean,
)
