package kr.devport.api.domain.article.dto.request

import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.enums.ItemType
import java.time.LocalDateTime

/**
 * Dynamic search filter for QueryDSL article search. Lives in the model module (not service) so the
 * QueryDSL adapter in :article:repository-jpa can reference it without depending on :article:service.
 */
data class ArticleSearchCondition(
    val category: Category? = null,
    val source: String? = null,
    val itemType: ItemType? = null,
    val keyword: String? = null,
    val minScore: Int? = null,
    val maxScore: Int? = null,
    val createdAfter: LocalDateTime? = null,
    val createdBefore: LocalDateTime? = null,
    val tags: List<String>? = null,
)
