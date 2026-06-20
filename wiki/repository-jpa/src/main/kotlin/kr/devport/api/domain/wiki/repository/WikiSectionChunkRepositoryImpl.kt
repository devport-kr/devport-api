package kr.devport.api.domain.wiki.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepositoryCustom.ScoredChunkRow
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Connection
import java.sql.ResultSet

@Repository
class WikiSectionChunkRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
) : WikiSectionChunkRepositoryCustom {
    private val objectMapper = ObjectMapper()

    private val scoredChunkRowMapper =
        RowMapper { rs, _ -> ScoredChunkRow(mapChunk(rs), rs.getDouble("score")) }

    override fun findSimilarChunksWithScore(
        projectExternalId: String,
        queryEmbedding: String,
        limit: Int,
    ): List<ScoredChunkRow> =
        jdbcTemplate.execute(
            ConnectionCallback { connection ->
                connection.prepareStatement(VECTOR_BY_PROJECT_SQL).use { statement ->
                    statement.setString(1, queryEmbedding)
                    statement.setString(2, projectExternalId)
                    statement.setString(3, queryEmbedding)
                    statement.setInt(4, limit)
                    statement.executeQuery().use { rs -> mapRows(rs) }
                }
            },
        )!!

    override fun findSimilarChunksGlobalWithScore(
        queryEmbedding: String,
        limit: Int,
    ): List<ScoredChunkRow> =
        jdbcTemplate.execute(
            ConnectionCallback { connection ->
                setVectorSearchEf(connection)
                try {
                    connection.prepareStatement(VECTOR_GLOBAL_SQL).use { statement ->
                        statement.setString(1, queryEmbedding)
                        statement.setString(2, queryEmbedding)
                        statement.setInt(3, limit)
                        statement.executeQuery().use { rs -> mapRows(rs) }
                    }
                } finally {
                    resetVectorSearchEf(connection)
                }
            },
        )!!

    override fun findLexicalCandidates(
        projectExternalId: String,
        question: String,
        limit: Int,
    ): List<ScoredChunkRow> =
        jdbcTemplate.query(
            LEXICAL_SQL,
            scoredChunkRowMapper,
            question,
            question,
            projectExternalId,
            question,
            question,
            limit,
        )

    private fun mapRows(rs: ResultSet): List<ScoredChunkRow> {
        val rows = mutableListOf<ScoredChunkRow>()
        var rowNum = 0
        while (rs.next()) {
            rows.add(scoredChunkRowMapper.mapRow(rs, rowNum++)!!)
        }
        return rows
    }

    private fun mapChunk(rs: ResultSet): WikiSectionChunk =
        WikiSectionChunk().apply {
            id = rs.getLong("id")
            projectExternalId = rs.getString("project_external_id")
            sectionId = rs.getString("section_id")
            subsectionId = rs.getString("subsection_id")
            chunkType = rs.getString("chunk_type")
            content = rs.getString("content")
            tokenCount = rs.getInt("token_count")
            metadata = readMetadata(rs.getObject("metadata"))
            commitSha = rs.getString("commit_sha")
        }

    private fun readMetadata(rawMetadata: Any?): MutableMap<String, Any>? {
        if (rawMetadata == null) {
            return null
        }
        val json = rawMetadata.toString()
        if (json.isBlank()) {
            return null
        }
        return try {
            objectMapper.readValue(json, object : TypeReference<MutableMap<String, Any>>() {})
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse wiki chunk metadata", e)
        }
    }

    private fun setVectorSearchEf(connection: Connection) {
        connection.createStatement().use { it.execute("SET hnsw.ef_search = $HNSW_EF_SEARCH") }
    }

    private fun resetVectorSearchEf(connection: Connection) {
        connection.createStatement().use { it.execute("RESET hnsw.ef_search") }
    }

    companion object {
        private const val HNSW_EF_SEARCH = 64

        private const val SELECT_CHUNK_COLUMNS = """
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
            """

        private const val VECTOR_BY_PROJECT_SQL =
            SELECT_CHUNK_COLUMNS +
                """
                , 1 - (c.embedding <=> cast(? AS vector)) AS score
                FROM wiki_section_chunks c
                WHERE c.project_external_id = ?
                ORDER BY c.embedding <=> cast(? AS vector)
                LIMIT ?
                """

        private const val VECTOR_GLOBAL_SQL =
            SELECT_CHUNK_COLUMNS +
                """
                , 1 - (c.embedding <=> cast(? AS vector)) AS score
                FROM wiki_section_chunks c
                ORDER BY c.embedding <=> cast(? AS vector)
                LIMIT ?
                """

        private const val LEXICAL_SQL =
            SELECT_CHUNK_COLUMNS +
                """
                ,
                (similarity(c.content, ?) + 0.5 * COALESCE(similarity(c.metadata->>'titleKo', ?), 0)) AS score
                FROM wiki_section_chunks c
                WHERE c.project_external_id = ?
                  AND (c.content % ? OR c.metadata->>'titleKo' % ?)
                ORDER BY score DESC
                LIMIT ?
                """
    }
}
