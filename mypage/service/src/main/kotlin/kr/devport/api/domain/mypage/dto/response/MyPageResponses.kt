package kr.devport.api.domain.mypage.dto.response

import kr.devport.api.domain.mypage.entity.UserReadHistory
import kr.devport.api.domain.mypage.entity.UserSavedArticle
import java.time.LocalDateTime

data class SavedArticleResponse(
    val articleId: String? = null,
    val summaryKoTitle: String? = null,
    val source: String? = null,
    val category: String? = null,
    val url: String? = null,
    val savedAt: LocalDateTime? = null,
) {
    companion object {
        fun from(saved: UserSavedArticle): SavedArticleResponse {
            val article = saved.article!!
            return SavedArticleResponse(
                articleId = article.externalId,
                summaryKoTitle = article.summaryKoTitle,
                source = article.source,
                category = article.category?.name,
                url = article.url,
                savedAt = saved.createdAt,
            )
        }
    }
}

data class ReadHistoryResponse(
    val articleId: String? = null,
    val summaryKoTitle: String? = null,
    val source: String? = null,
    val category: String? = null,
    val url: String? = null,
    val readAt: LocalDateTime? = null,
) {
    companion object {
        fun from(history: UserReadHistory): ReadHistoryResponse {
            val article = history.article!!
            return ReadHistoryResponse(
                articleId = article.externalId,
                summaryKoTitle = article.summaryKoTitle,
                source = article.source,
                category = article.category?.name,
                url = article.url,
                readAt = history.readAt,
            )
        }
    }
}
