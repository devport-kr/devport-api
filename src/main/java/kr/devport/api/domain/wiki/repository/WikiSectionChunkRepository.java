package kr.devport.api.domain.wiki.repository;

import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WikiSectionChunkRepository extends JpaRepository<WikiSectionChunk, Long> {

    List<WikiSectionChunk> findByProjectExternalId(String projectExternalId);

    void deleteByProjectExternalId(String projectExternalId);

    @Query(value = """
            SELECT c.* FROM wiki_section_chunks c
            WHERE c.project_external_id = :projectExternalId
            ORDER BY c.embedding <=> cast(:queryEmbedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<WikiSectionChunk> findSimilarChunks(
            @Param("projectExternalId") String projectExternalId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit);
}
