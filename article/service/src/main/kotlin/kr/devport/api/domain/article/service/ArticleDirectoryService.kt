package kr.devport.api.domain.article.service

import kr.devport.api.domain.article.ArticleView
import kr.devport.api.domain.article.entity.Article
import kr.devport.api.domain.article.infrastructure.ArticleDirectory
import kr.devport.api.domain.article.infrastructure.ArticleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Implements the cross-domain inbound port: resolves articles to [ArticleView] for other domains. */
@Service
@Transactional(readOnly = true)
class ArticleDirectoryService(
    private val articleRepository: ArticleRepository,
) : ArticleDirectory {
    override fun findByExternalId(externalId: String): ArticleView? = articleRepository.findByExternalId(externalId)?.toView()

    override fun findByIds(ids: Collection<Long>): Map<Long, ArticleView> {
        if (ids.isEmpty()) return emptyMap()
        return articleRepository.findAllByIdIn(ids).associate { it.id!! to it.toView() }
    }
}

private fun Article.toView(): ArticleView =
    ArticleView(
        id = id!!,
        externalId = externalId,
        summaryKoTitle = summaryKoTitle,
        source = source,
        category = category?.name,
        url = url,
    )
