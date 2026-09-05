package kr.devport.api.domain.article.infrastructure

import kr.devport.api.domain.article.ArticleView

/**
 * Inbound port: the article core's published contract for *other* domains. Replaces cross-domain
 * reach into article's entities/repositories. Implemented by :article:service.
 */
interface ArticleDirectory {
    fun findByExternalId(externalId: String): ArticleView?

    fun findByIds(ids: Collection<Long>): Map<Long, ArticleView>
}
