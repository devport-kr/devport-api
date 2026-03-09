package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Wiki chat retrieval service using vector similarity search over wiki_section_chunks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiRetrievalService {

    private static final int CANDIDATE_LIMIT = 8;
    private static final int FAQ_CANDIDATE_LIMIT = 16;
    private static final int MAX_CONTEXT_CHUNKS = 4;
    private static final int MAX_CONTEXT_TOKENS = 3000;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9가-힣]+");

    // FAQ question patterns — matched in order; first match wins
    private static final Pattern FAQ_PROBLEM    = Pattern.compile(
            "문제.*해결|해결.*문제|왜 만들|어떤.*목적|what.*problem|solve|why.*built",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FAQ_ARCH       = Pattern.compile(
            "아키텍처|구조|설계|architecture|design|어떻게.*동작|동작.*원리|how.*work|내부.*구조",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FAQ_START      = Pattern.compile(
            "시작하려면|어떻게.*시작|설치|install|setup|getting.?started|사용.*방법|how.*use|how.*start",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FAQ_CHANGES    = Pattern.compile(
            "최근.*변경|변경.*사항|changelog|release|업데이트|최근.*업|새로운|recent.*change|what.*new",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FAQ_FEATURES   = Pattern.compile(
            "주요.*기능|기능.*무엇|특징|feature|capabilities|what.*can|뭘.*할|어떤.*기능",
            Pattern.CASE_INSENSITIVE);

    private enum FaqType { PROBLEM_SOLVED, ARCHITECTURE, GETTING_STARTED, RECENT_CHANGES, KEY_FEATURES, NONE }

    private final WikiSectionChunkRepository chunkRepository;
    private final OpenAIClient openAIClient;

    /**
     * Retrieve grounded context for a chat turn via vector similarity search.
     * Throws if no chunks exist for this project.
     */
    public WikiRetrievalContext retrieveContext(String projectExternalId, String userQuestion) {
        List<WikiSectionChunk> allChunks = chunkRepository.findByProjectExternalId(projectExternalId);
        if (allChunks.isEmpty()) {
            throw new IllegalArgumentException("No wiki content found for project: " + projectExternalId);
        }

        try {
            FaqType faqType = detectFaq(userQuestion);
            int candidateLimit = faqType != FaqType.NONE ? FAQ_CANDIDATE_LIMIT : CANDIDATE_LIMIT;

            float[] questionEmbedding = embedText(userQuestion);
            String vectorStr = toVectorString(questionEmbedding);

            List<WikiSectionChunk> similarChunks = chunkRepository.findSimilarChunks(
                    projectExternalId, vectorStr, candidateLimit);

            if (similarChunks.isEmpty()) {
                return buildWeakGroundingContext(projectExternalId, allChunks, userQuestion);
            }

            List<ScoredChunk> selectedChunks = selectDiverseChunks(similarChunks, userQuestion, faqType);
            boolean weakGrounding = selectedChunks.size() < 2;

            return new WikiRetrievalContext(
                    projectExternalId,
                    buildGroundedContext(selectedChunks),
                    !selectedChunks.isEmpty(),
                    weakGrounding,
                    toRetrievedChunks(selectedChunks),
                    weakGrounding ? suggestNextQuestions(userQuestion, selectedChunks) : List.of()
            );
        } catch (Exception e) {
            log.warn("Vector search failed for project {}: {}", projectExternalId, e.getMessage());
            return buildWeakGroundingContext(projectExternalId, allChunks, userQuestion);
        }
    }

    private WikiRetrievalContext buildWeakGroundingContext(
            String projectExternalId,
            List<WikiSectionChunk> allChunks,
            String userQuestion
    ) {
        List<ScoredChunk> fallbackChunks = selectFallbackChunks(allChunks, userQuestion);

        return new WikiRetrievalContext(
                projectExternalId,
                buildGroundedContext(fallbackChunks),
                !fallbackChunks.isEmpty(),
                true,
                toRetrievedChunks(fallbackChunks),
                suggestNextQuestions(userQuestion, fallbackChunks)
        );
    }

    private List<ScoredChunk> selectDiverseChunks(
            List<WikiSectionChunk> candidates, String userQuestion, FaqType faqType) {
        List<ScoredChunk> scoredCandidates = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            WikiSectionChunk chunk = candidates.get(i);
            double similarityScore = Math.max(0.1d, 1.0d - (i * 0.04d));
            double headingScore = computeHeadingScore(chunk, userQuestion);
            double faqBonus = faqChunkBonus(faqType, chunk);
            double qualityScore = similarityScore + headingScore + faqBonus;
            scoredCandidates.add(new ScoredChunk(chunk, similarityScore, headingScore, qualityScore));
        }

        return greedySelect(scoredCandidates);
    }

    private List<ScoredChunk> selectFallbackChunks(List<WikiSectionChunk> allChunks, String userQuestion) {
        List<ScoredChunk> scored = allChunks.stream()
                .sorted(Comparator
                        .comparingInt((WikiSectionChunk chunk) -> fallbackChunkPriority(chunk.getChunkType()))
                        .thenComparing(WikiSectionChunk::getSectionId)
                        .thenComparing(chunk -> chunk.getSubsectionId() == null ? "" : chunk.getSubsectionId()))
                .map(chunk -> new ScoredChunk(chunk, 0.35d, computeHeadingScore(chunk, userQuestion), 0.35d))
                .toList();

        return greedySelect(scored);
    }

    private List<ScoredChunk> greedySelect(List<ScoredChunk> candidates) {
        List<ScoredChunk> remaining = new ArrayList<>(candidates);
        List<ScoredChunk> selected = new ArrayList<>();
        Set<String> usedSections = new LinkedHashSet<>();

        while (!remaining.isEmpty() && selected.size() < MAX_CONTEXT_CHUNKS) {
            ScoredChunk next = remaining.stream()
                    .max(Comparator.comparingDouble(candidate -> rerankScore(candidate, usedSections)))
                    .orElse(null);
            if (next == null) {
                break;
            }
            selected.add(next);
            usedSections.add(next.chunk().getSectionId());
            remaining.remove(next);
        }

        return selected;
    }

    private double rerankScore(ScoredChunk candidate, Set<String> usedSections) {
        double diversityBonus = usedSections.contains(candidate.chunk().getSectionId()) ? 0.0d : 0.35d;
        return candidate.qualityScore() + diversityBonus;
    }

    private int fallbackChunkPriority(String chunkType) {
        if ("summary".equals(chunkType)) {
            return 0;
        }
        if ("overview".equals(chunkType)) {
            return 1;
        }
        return 2;
    }

    private String buildGroundedContext(List<ScoredChunk> chunks) {
        StringBuilder context = new StringBuilder();
        context.append("# Repository Context\n\n");
        for (ScoredChunk chunk : chunks) {
            context.append("## ").append(resolveHeading(chunk.chunk())).append("\n\n");
            context.append(chunk.chunk().getContent()).append("\n\n");
        }
        return truncateToTokenLimit(context.toString(), MAX_CONTEXT_TOKENS);
    }

    private List<WikiRetrievedChunk> toRetrievedChunks(List<ScoredChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new WikiRetrievedChunk(
                        chunk.chunk().getSectionId(),
                        chunk.chunk().getSubsectionId(),
                        resolveHeading(chunk.chunk()),
                        chunk.chunk().getContent(),
                        chunk.similarityScore(),
                        chunk.headingScore(),
                        resolveSourcePathHint(chunk.chunk())
                ))
                .toList();
    }

    private String resolveHeading(WikiSectionChunk chunk) {
        Map<String, Object> metadata = chunk.getMetadata();
        if (metadata != null) {
            Object titleKo = metadata.get("titleKo");
            if (titleKo != null && !String.valueOf(titleKo).isBlank()) {
                return String.valueOf(titleKo);
            }
        }
        if (chunk.getSubsectionId() != null && !chunk.getSubsectionId().isBlank()) {
            return chunk.getSectionId() + " > " + chunk.getSubsectionId();
        }
        return chunk.getSectionId();
    }

    private String resolveSourcePathHint(WikiSectionChunk chunk) {
        Map<String, Object> metadata = chunk.getMetadata();
        if (metadata == null) {
            return null;
        }
        Object sourcePath = metadata.get("sourcePath");
        if (sourcePath != null && !String.valueOf(sourcePath).isBlank()) {
            return String.valueOf(sourcePath);
        }
        return null;
    }

    private double computeHeadingScore(WikiSectionChunk chunk, String userQuestion) {
        Set<String> questionTokens = tokenize(userQuestion);
        if (questionTokens.isEmpty()) {
            return 0.0d;
        }

        Set<String> headingTokens = tokenize(resolveHeading(chunk) + " " + chunk.getSectionId());
        long overlap = questionTokens.stream()
                .filter(questionToken -> headingTokens.stream().anyMatch(headingToken -> matchesToken(questionToken, headingToken)))
                .count();

        return overlap / (double) questionTokens.size();
    }

    private Set<String> tokenize(String text) {
        return TOKEN_SPLIT.splitAsStream(text.toLowerCase(Locale.ROOT))
                .filter(token -> token.length() >= 2)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private boolean matchesToken(String left, String right) {
        return left.equals(right) || left.startsWith(right) || right.startsWith(left);
    }

    private FaqType detectFaq(String question) {
        if (FAQ_PROBLEM.matcher(question).find())  return FaqType.PROBLEM_SOLVED;
        if (FAQ_ARCH.matcher(question).find())     return FaqType.ARCHITECTURE;
        if (FAQ_START.matcher(question).find())    return FaqType.GETTING_STARTED;
        if (FAQ_CHANGES.matcher(question).find())  return FaqType.RECENT_CHANGES;
        if (FAQ_FEATURES.matcher(question).find()) return FaqType.KEY_FEATURES;
        return FaqType.NONE;
    }

    /**
     * Returns a bonus score for a chunk based on the detected FAQ type.
     * Only boosts summary chunks for purpose/feature questions — the only reliable
     * discriminator given chunk_type has just 3 values and sectionIds are numeric.
     * Everything else is handled by vector similarity + LLM instruction.
     */
    private double faqChunkBonus(FaqType faqType, WikiSectionChunk chunk) {
        String chunkType = chunk.getChunkType();
        return switch (faqType) {
            case PROBLEM_SOLVED, KEY_FEATURES -> "summary".equals(chunkType) ? 0.5d : 0.0d;
            default -> 0.0d;
        };
    }

    private List<String> suggestNextQuestions(String userQuestion, List<ScoredChunk> chunks) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (ScoredChunk chunk : chunks) {
            suggestions.add(resolveHeading(chunk.chunk()) + " 기준으로 설명해줘");
            if (suggestions.size() == 3) {
                break;
            }
            String sourcePathHint = resolveSourcePathHint(chunk.chunk());
            if (sourcePathHint != null) {
                suggestions.add(sourcePathHint + " 관련 흐름을 알려줘");
            }
            if (suggestions.size() == 3) {
                break;
            }
        }
        if (suggestions.size() < 2) {
            suggestions.add("이 질문과 직접 관련된 클래스나 파일 경로를 알려줘");
        }
        if (suggestions.size() < 2) {
            suggestions.add("관련 섹션 이름을 기준으로 다시 설명해줘");
        }
        if (suggestions.size() < 3) {
            suggestions.add("질문을 더 좁혀서 어떤 부분이 궁금한지 알려줘");
        }
        return suggestions.stream().limit(3).toList();
    }

    private float[] embedText(String text) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model("text-embedding-3-small")
                .input(text)
                .build();

        CreateEmbeddingResponse response = openAIClient.embeddings().create(params);

        List<Float> embeddingFloats = response.data().getFirst().embedding();
        float[] embedding = new float[embeddingFloats.size()];
        for (int i = 0; i < embeddingFloats.size(); i++) {
            embedding[i] = embeddingFloats.get(i);
        }
        return embedding;
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String truncateToTokenLimit(String text, int maxTokens) {
        int maxChars = maxTokens * 4;
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "...";
    }

    private record ScoredChunk(
            WikiSectionChunk chunk,
            double similarityScore,
            double headingScore,
            double qualityScore
    ) {
    }
}
