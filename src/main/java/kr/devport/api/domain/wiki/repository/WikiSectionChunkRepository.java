package kr.devport.api.domain.wiki.repository;

import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WikiSectionChunkRepository extends JpaRepository<WikiSectionChunk, Long>, WikiSectionChunkRepositoryCustom {

    List<WikiSectionChunk> findByProjectExternalId(String projectExternalId);

    @Query("SELECT c FROM WikiSectionChunk c WHERE c.chunkType = 'summary' ORDER BY c.id ASC")
    List<WikiSectionChunk> findAllSummaryChunks();

    void deleteByProjectExternalId(String projectExternalId);
}
