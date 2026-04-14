package kr.devport.api.domain.wiki.repository;

import kr.devport.api.domain.wiki.entity.WikiSectionChunk;

import java.util.List;

public interface WikiSectionChunkRepositoryCustom {

    List<ScoredChunkRow> findSimilarChunksWithScore(String projectExternalId, String queryEmbedding, int limit);

    List<ScoredChunkRow> findSimilarChunksGlobalWithScore(String queryEmbedding, int limit);

    List<ScoredChunkRow> findLexicalCandidates(String projectExternalId, String question, int limit);

    record ScoredChunkRow(WikiSectionChunk chunk, double score) {
    }
}
