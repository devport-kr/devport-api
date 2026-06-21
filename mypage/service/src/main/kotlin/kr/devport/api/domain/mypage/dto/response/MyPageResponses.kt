package kr.devport.api.domain.mypage.dto.response

import kr.devport.api.domain.article.ArticleView
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
        fun from(
            saved: UserSavedArticle,
            article: ArticleView?,
        ): SavedArticleResponse =
            SavedArticleResponse(
                articleId = article?.externalId,
                summaryKoTitle = article?.summaryKoTitle,
                source = article?.source,
                category = article?.category,
                url = article?.url,
                savedAt = saved.createdAt,
            )
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
        fun from(
            history: UserReadHistory,
            article: ArticleView?,
        ): ReadHistoryResponse =
            ReadHistoryResponse(
                articleId = article?.externalId,
                summaryKoTitle = article?.summaryKoTitle,
                source = article?.source,
                category = article?.category,
                url = article?.url,
                readAt = history.readAt,
            )
    }
}
