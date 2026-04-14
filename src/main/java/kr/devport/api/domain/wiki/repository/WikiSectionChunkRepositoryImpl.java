package kr.devport.api.domain.wiki.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class WikiSectionChunkRepositoryImpl implements WikiSectionChunkRepositoryCustom {

    private static final int HNSW_EF_SEARCH = 64;

    private static final String SELECT_CHUNK_COLUMNS = """
            SELECT
                c.id,
                c.project_external_id,
                c.section_id,
                c.subsection_id,
                c.chunk_type,
                c.content,
                c.token_count,
                c.metadata,
                c.commit_sha
            """;

    private static final String VECTOR_BY_PROJECT_SQL = SELECT_CHUNK_COLUMNS + """
            , 1 - (c.embedding <=> cast(? AS vector)) AS score
            FROM wiki_section_chunks c
            WHERE c.project_external_id = ?
            ORDER BY c.embedding <=> cast(? AS vector)
            LIMIT ?
            """;

    private static final String VECTOR_GLOBAL_SQL = SELECT_CHUNK_COLUMNS + """
            , 1 - (c.embedding <=> cast(? AS vector)) AS score
            FROM wiki_section_chunks c
            ORDER BY c.embedding <=> cast(? AS vector)
            LIMIT ?
            """;

    private static final String LEXICAL_SQL = SELECT_CHUNK_COLUMNS + """
            ,
            (similarity(c.content, ?) + 0.5 * COALESCE(similarity(c.metadata->>'titleKo', ?), 0)) AS score
            FROM wiki_section_chunks c
            WHERE c.project_external_id = ?
              AND (c.content % ? OR c.metadata->>'titleKo' % ?)
            ORDER BY score DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RowMapper<ScoredChunkRow> scoredChunkRowMapper = (rs, rowNum) ->
            new ScoredChunkRow(mapChunk(rs), rs.getDouble("score"));

    @Override
    public List<ScoredChunkRow> findSimilarChunksWithScore(String projectExternalId, String queryEmbedding, int limit) {
        return jdbcTemplate.execute((ConnectionCallback<List<ScoredChunkRow>>) connection -> {
            setVectorSearchEf(connection);
            try (PreparedStatement statement = connection.prepareStatement(VECTOR_BY_PROJECT_SQL)) {
                statement.setString(1, queryEmbedding);
                statement.setString(2, projectExternalId);
                statement.setString(3, queryEmbedding);
                statement.setInt(4, limit);
                try (ResultSet rs = statement.executeQuery()) {
                    return mapRows(rs);
                }
            } finally {
                resetVectorSearchEf(connection);
            }
        });
    }

    @Override
    public List<ScoredChunkRow> findSimilarChunksGlobalWithScore(String queryEmbedding, int limit) {
        return jdbcTemplate.execute((ConnectionCallback<List<ScoredChunkRow>>) connection -> {
            setVectorSearchEf(connection);
            try (PreparedStatement statement = connection.prepareStatement(VECTOR_GLOBAL_SQL)) {
                statement.setString(1, queryEmbedding);
                statement.setString(2, queryEmbedding);
                statement.setInt(3, limit);
                try (ResultSet rs = statement.executeQuery()) {
                    return mapRows(rs);
                }
            } finally {
                resetVectorSearchEf(connection);
            }
        });
    }

    @Override
    public List<ScoredChunkRow> findLexicalCandidates(String projectExternalId, String question, int limit) {
        return jdbcTemplate.query(
                LEXICAL_SQL,
                scoredChunkRowMapper,
                question,
                question,
                projectExternalId,
                question,
                question,
                limit
        );
    }

    private List<ScoredChunkRow> mapRows(ResultSet rs) throws SQLException {
        List<ScoredChunkRow> rows = new java.util.ArrayList<>();
        int rowNum = 0;
        while (rs.next()) {
            rows.add(scoredChunkRowMapper.mapRow(rs, rowNum++));
        }
        return rows;
    }

    private WikiSectionChunk mapChunk(ResultSet rs) throws SQLException {
        return WikiSectionChunk.builder()
                .id(rs.getLong("id"))
                .projectExternalId(rs.getString("project_external_id"))
                .sectionId(rs.getString("section_id"))
                .subsectionId(rs.getString("subsection_id"))
                .chunkType(rs.getString("chunk_type"))
                .content(rs.getString("content"))
                .tokenCount(rs.getInt("token_count"))
                .metadata(readMetadata(rs.getObject("metadata")))
                .commitSha(rs.getString("commit_sha"))
                .build();
    }

    private Map<String, Object> readMetadata(Object rawMetadata) {
        if (rawMetadata == null) {
            return null;
        }
        String json = String.valueOf(rawMetadata);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse wiki chunk metadata", e);
        }
    }

    private void setVectorSearchEf(java.sql.Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET hnsw.ef_search = " + HNSW_EF_SEARCH);
        }
    }

    private void resetVectorSearchEf(java.sql.Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("RESET hnsw.ef_search");
        }
    }
}
