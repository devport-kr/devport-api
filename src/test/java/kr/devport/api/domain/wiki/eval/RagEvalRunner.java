package kr.devport.api.domain.wiki.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk;
import kr.devport.api.domain.wiki.service.WikiRetrievalService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Tag("rag-eval")
@SpringBootTest
@ActiveProfiles("rag-eval")
@EnabledIfSystemProperty(named = "rag-eval", matches = "true")
class RagEvalRunner {

    private static final Path FIXTURES_PATH = Path.of("src/test/resources/rag-eval/fixtures.json");
    private static final Path OUTPUT_PATH = Path.of("build/rag-eval/results.json");

    @Autowired
    private WikiRetrievalService retrievalService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void runEvaluation() throws Exception {
        String databaseUrl = System.getenv("RAG_EVAL_DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("RAG_EVAL_DATABASE_URL must be set for ragEval.");
        }

        List<Fixture> fixtures = objectMapper.readValue(FIXTURES_PATH.toFile(), new TypeReference<>() {
        });
        if (fixtures.isEmpty()) {
            throw new IllegalStateException("No rag-eval fixtures found. Populate src/test/resources/rag-eval/fixtures.json.");
        }

        List<QuestionResult> questionResults = new ArrayList<>();
        int skipped = 0;

        for (Fixture fixture : fixtures) {
            try {
                List<WikiRetrievedChunk> chunks = retrievalService
                        .retrieveContext(fixture.projectExternalId(), fixture.question())
                        .chunks();
                questionResults.add(scoreFixture(fixture, chunks));
            } catch (IllegalArgumentException e) {
                skipped++;
                questionResults.add(QuestionResult.skipped(fixture, e.getMessage()));
            }
        }

        Summary summary = summarize(questionResults, skipped);
        Files.createDirectories(OUTPUT_PATH.getParent());
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(OUTPUT_PATH.toFile(), new ResultEnvelope(summary, questionResults));

        System.out.printf(Locale.ROOT,
                "rag-eval fixtures=%d evaluated=%d skipped=%d hit@1=%.3f hit@3=%.3f hit@5=%.3f mrr=%.3f%n",
                fixtures.size(),
                summary.evaluated(),
                summary.skipped(),
                summary.hitAt1(),
                summary.hitAt3(),
                summary.hitAt5(),
                summary.mrr());
    }

    private QuestionResult scoreFixture(Fixture fixture, List<WikiRetrievedChunk> chunks) {
        Set<String> expectedSections = Set.copyOf(fixture.expectedSectionIds());
        Set<String> expectedChunkTypes = Set.copyOf(fixture.expectedChunkTypes());

        int firstRelevantRank = -1;
        boolean hitAt1 = false;
        boolean hitAt3 = false;
        boolean hitAt5 = false;

        for (int i = 0; i < chunks.size(); i++) {
            WikiRetrievedChunk chunk = chunks.get(i);
            boolean matchesSection = expectedSections.contains(chunk.sectionId());
            boolean matchesChunkType = expectedChunkTypes.isEmpty()
                    || expectedChunkTypes.contains(chunk.chunkType());

            if (matchesSection && matchesChunkType) {
                firstRelevantRank = i + 1;
                hitAt1 = firstRelevantRank <= 1;
                hitAt3 = firstRelevantRank <= 3;
                hitAt5 = firstRelevantRank <= 5;
                break;
            }
        }

        double reciprocalRank = firstRelevantRank > 0 ? 1.0d / firstRelevantRank : 0.0d;
        return new QuestionResult(
                fixture.projectExternalId(),
                fixture.question(),
                fixture.expectedSectionIds(),
                fixture.expectedChunkTypes(),
                chunks.stream().map(chunk -> new RetrievedChunkResult(
                        chunk.sectionId(),
                        chunk.chunkType(),
                        chunk.heading(),
                        chunk.similarityScore(),
                        chunk.rerankScore()
                )).toList(),
                hitAt1,
                hitAt3,
                hitAt5,
                reciprocalRank,
                firstRelevantRank,
                false,
                null
        );
    }

    private Summary summarize(List<QuestionResult> results, int skipped) {
        List<QuestionResult> evaluated = results.stream()
                .filter(result -> !result.skipped())
                .toList();
        int evaluatedCount = evaluated.size();
        if (evaluatedCount == 0) {
            return new Summary(0, skipped, 0.0d, 0.0d, 0.0d, 0.0d);
        }

        double hitAt1 = evaluated.stream().filter(QuestionResult::hitAt1).count() / (double) evaluatedCount;
        double hitAt3 = evaluated.stream().filter(QuestionResult::hitAt3).count() / (double) evaluatedCount;
        double hitAt5 = evaluated.stream().filter(QuestionResult::hitAt5).count() / (double) evaluatedCount;
        double mrr = evaluated.stream().mapToDouble(QuestionResult::reciprocalRank).average().orElse(0.0d);
        return new Summary(evaluatedCount, skipped, hitAt1, hitAt3, hitAt5, mrr);
    }

    private record Fixture(
            String projectExternalId,
            String question,
            List<String> expectedSectionIds,
            List<String> expectedChunkTypes
    ) {
    }

    private record RetrievedChunkResult(
            String sectionId,
            String chunkType,
            String heading,
            double similarityScore,
            Double rerankScore
    ) {
    }

    private record QuestionResult(
            String projectExternalId,
            String question,
            List<String> expectedSectionIds,
            List<String> expectedChunkTypes,
            List<RetrievedChunkResult> retrievedChunks,
            boolean hitAt1,
            boolean hitAt3,
            boolean hitAt5,
            double reciprocalRank,
            int firstRelevantRank,
            boolean skipped,
            String skipReason
    ) {
        private static QuestionResult skipped(Fixture fixture, String reason) {
            return new QuestionResult(
                    fixture.projectExternalId(),
                    fixture.question(),
                    fixture.expectedSectionIds(),
                    fixture.expectedChunkTypes(),
                    List.of(),
                    false,
                    false,
                    false,
                    0.0d,
                    -1,
                    true,
                    reason
            );
        }
    }

    private record Summary(
            int evaluated,
            int skipped,
            double hitAt1,
            double hitAt3,
            double hitAt5,
            double mrr
    ) {
    }

    private record ResultEnvelope(
            Summary summary,
            List<QuestionResult> questions
    ) {
    }
}
