package kr.devport.api.domain.llm.service.admin;

import kr.devport.api.domain.llm.entity.LLMModel;
import kr.devport.api.domain.llm.entity.ModelCreator;
import kr.devport.api.domain.llm.dto.request.admin.LLMModelCreateRequest;
import kr.devport.api.domain.llm.dto.request.admin.LLMModelUpdateRequest;
import kr.devport.api.domain.llm.dto.response.LLMModelDetailResponse;
import kr.devport.api.domain.llm.dto.response.ModelCreatorResponse;
import kr.devport.api.domain.llm.repository.LLMModelRepository;
import kr.devport.api.domain.llm.repository.ModelCreatorRepository;
import lombok.RequiredArgsConstructor;
import kr.devport.api.domain.common.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LLMModelAdminService {

    private final LLMModelRepository llmModelRepository;
    private final ModelCreatorRepository modelCreatorRepository;

    @CacheEvict(value = {
        CacheNames.LLM_MODELS,
        CacheNames.LLM_LEADERBOARD
    }, allEntries = true)
    public LLMModelDetailResponse createLLMModel(LLMModelCreateRequest request) {
        LLMModel model = LLMModel.builder()
            .externalId(request.getExternalId())
            .slug(request.getSlug())
            .modelId(request.getModelId())
            .modelName(request.getModelName())
            .releaseDate(request.getReleaseDate())
            .provider(request.getProvider())
            .description(request.getDescription())
            .priceInput(request.getPriceInput())
            .priceOutput(request.getPriceOutput())
            .priceBlended(request.getPriceBlended())
            .contextWindow(request.getContextWindow())
            .outputSpeedMedian(request.getOutputSpeedMedian())
            .latencyTtft(request.getLatencyTtft())
            .medianTimeToFirstAnswerToken(request.getMedianTimeToFirstAnswerToken())
            .license(request.getLicense())
            .scoreTerminalBenchHard(request.getScoreTerminalBenchHard())
            .scoreTauBenchTelecom(request.getScoreTauBenchTelecom())
            .scoreAaLcr(request.getScoreAaLcr())
            .scoreHumanitysLastExam(request.getScoreHumanitysLastExam())
            .scoreMmluPro(request.getScoreMmluPro())
            .scoreGpqaDiamond(request.getScoreGpqaDiamond())
            .scoreLivecodeBench(request.getScoreLivecodeBench())
            .scoreScicode(request.getScoreScicode())
            .scoreIfbench(request.getScoreIfbench())
            .scoreMath500(request.getScoreMath500())
            .scoreAime(request.getScoreAime())
            .scoreAime2025(request.getScoreAime2025())
            .scoreAaIntelligenceIndex(request.getScoreAaIntelligenceIndex())
            .scoreAaCodingIndex(request.getScoreAaCodingIndex())
            .scoreAaMathIndex(request.getScoreAaMathIndex())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        if (request.getModelCreatorId() != null) {
            ModelCreator creator = modelCreatorRepository.findById(request.getModelCreatorId())
                .orElseThrow(() -> new IllegalArgumentException("ModelCreator not found with id: " + request.getModelCreatorId()));
            model.setModelCreator(creator);
        }

        LLMModel saved = llmModelRepository.save(model);
        return convertToDetailResponse(saved);
    }

    @CacheEvict(value = {
        CacheNames.LLM_MODELS,
        CacheNames.LLM_LEADERBOARD
    }, allEntries = true)
    public LLMModelDetailResponse updateLLMModel(Long id, LLMModelUpdateRequest request) {
        LLMModel model = llmModelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("LLMModel not found with id: " + id));

        if (request.getExternalId() != null) model.setExternalId(request.getExternalId());
        if (request.getSlug() != null) model.setSlug(request.getSlug());
        if (request.getModelId() != null) model.setModelId(request.getModelId());
        if (request.getModelName() != null) model.setModelName(request.getModelName());
        if (request.getReleaseDate() != null) model.setReleaseDate(request.getReleaseDate());
        if (request.getProvider() != null) model.setProvider(request.getProvider());
        if (request.getDescription() != null) model.setDescription(request.getDescription());
        if (request.getPriceInput() != null) model.setPriceInput(request.getPriceInput());
        if (request.getPriceOutput() != null) model.setPriceOutput(request.getPriceOutput());
        if (request.getPriceBlended() != null) model.setPriceBlended(request.getPriceBlended());
        if (request.getContextWindow() != null) model.setContextWindow(request.getContextWindow());
        if (request.getOutputSpeedMedian() != null) model.setOutputSpeedMedian(request.getOutputSpeedMedian());
        if (request.getLatencyTtft() != null) model.setLatencyTtft(request.getLatencyTtft());
        if (request.getMedianTimeToFirstAnswerToken() != null) model.setMedianTimeToFirstAnswerToken(request.getMedianTimeToFirstAnswerToken());
        if (request.getLicense() != null) model.setLicense(request.getLicense());
        if (request.getScoreTerminalBenchHard() != null) model.setScoreTerminalBenchHard(request.getScoreTerminalBenchHard());
        if (request.getScoreTauBenchTelecom() != null) model.setScoreTauBenchTelecom(request.getScoreTauBenchTelecom());
        if (request.getScoreAaLcr() != null) model.setScoreAaLcr(request.getScoreAaLcr());
        if (request.getScoreHumanitysLastExam() != null) model.setScoreHumanitysLastExam(request.getScoreHumanitysLastExam());
        if (request.getScoreMmluPro() != null) model.setScoreMmluPro(request.getScoreMmluPro());
        if (request.getScoreGpqaDiamond() != null) model.setScoreGpqaDiamond(request.getScoreGpqaDiamond());
        if (request.getScoreLivecodeBench() != null) model.setScoreLivecodeBench(request.getScoreLivecodeBench());
        if (request.getScoreScicode() != null) model.setScoreScicode(request.getScoreScicode());
        if (request.getScoreIfbench() != null) model.setScoreIfbench(request.getScoreIfbench());
        if (request.getScoreMath500() != null) model.setScoreMath500(request.getScoreMath500());
        if (request.getScoreAime() != null) model.setScoreAime(request.getScoreAime());
        if (request.getScoreAime2025() != null) model.setScoreAime2025(request.getScoreAime2025());
        if (request.getScoreAaIntelligenceIndex() != null) model.setScoreAaIntelligenceIndex(request.getScoreAaIntelligenceIndex());
        if (request.getScoreAaCodingIndex() != null) model.setScoreAaCodingIndex(request.getScoreAaCodingIndex());
        if (request.getScoreAaMathIndex() != null) model.setScoreAaMathIndex(request.getScoreAaMathIndex());

        if (request.getModelCreatorId() != null) {
            ModelCreator creator = modelCreatorRepository.findById(request.getModelCreatorId())
                .orElseThrow(() -> new IllegalArgumentException("ModelCreator not found with id: " + request.getModelCreatorId()));
            model.setModelCreator(creator);
        }

        model.setUpdatedAt(LocalDateTime.now());
        LLMModel updated = llmModelRepository.save(model);
        return convertToDetailResponse(updated);
    }

    @CacheEvict(value = {
        CacheNames.LLM_MODELS,
        CacheNames.LLM_LEADERBOARD
    }, allEntries = true)
    public void deleteLLMModel(Long id) {
        if (!llmModelRepository.existsById(id)) {
            throw new IllegalArgumentException("LLMModel not found with id: " + id);
        }
        llmModelRepository.deleteById(id);
    }

    private LLMModelDetailResponse convertToDetailResponse(LLMModel model) {
        ModelCreatorResponse creatorResponse = null;
        if (model.getModelCreator() != null) {
            ModelCreator creator = model.getModelCreator();
            creatorResponse = ModelCreatorResponse.builder()
                .id(creator.getId())
                .externalId(creator.getExternalId())
                .slug(creator.getSlug())
                .name(creator.getName())
                .build();
        }

        return LLMModelDetailResponse.builder()
            .id(model.getId())
            .externalId(model.getExternalId())
            .slug(model.getSlug())
            .modelId(model.getModelId())
            .modelName(model.getModelName())
            .releaseDate(model.getReleaseDate())
            .provider(model.getProvider())
            .modelCreator(creatorResponse)
            .description(model.getDescription())
            .priceInput(model.getPriceInput())
            .priceOutput(model.getPriceOutput())
            .priceBlended(model.getPriceBlended())
            .contextWindow(model.getContextWindow())
            .outputSpeedMedian(model.getOutputSpeedMedian())
            .latencyTtft(model.getLatencyTtft())
            .medianTimeToFirstAnswerToken(model.getMedianTimeToFirstAnswerToken())
            .license(model.getLicense())
            .scoreTerminalBenchHard(model.getScoreTerminalBenchHard())
            .scoreTauBenchTelecom(model.getScoreTauBenchTelecom())
            .scoreAaLcr(model.getScoreAaLcr())
            .scoreHumanitysLastExam(model.getScoreHumanitysLastExam())
            .scoreMmluPro(model.getScoreMmluPro())
            .scoreGpqaDiamond(model.getScoreGpqaDiamond())
            .scoreLivecodeBench(model.getScoreLivecodeBench())
            .scoreScicode(model.getScoreScicode())
            .scoreIfbench(model.getScoreIfbench())
            .scoreMath500(model.getScoreMath500())
            .scoreAime(model.getScoreAime())
            .scoreAime2025(model.getScoreAime2025())
            .scoreAaIntelligenceIndex(model.getScoreAaIntelligenceIndex())
            .scoreAaCodingIndex(model.getScoreAaCodingIndex())
            .scoreAaMathIndex(model.getScoreAaMathIndex())
            .build();
    }
}
