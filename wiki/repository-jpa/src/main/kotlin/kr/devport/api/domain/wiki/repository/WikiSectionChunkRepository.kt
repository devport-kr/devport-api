package kr.devport.api.domain.wiki.repository

import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface WikiSectionChunkRepository :
    JpaRepository<WikiSectionChunk, Long>,
    WikiSectionChunkRepositoryCustom {
    fun findByProjectExternalId(projectExternalId: String): List<WikiSectionChunk>

    @Query("SELECT c FROM WikiSectionChunk c WHERE c.chunkType = 'summary' ORDER BY c.id ASC")
    fun findAllSummaryChunks(): List<WikiSectionChunk>

    fun deleteByProjectExternalId(projectExternalId: String)
}
