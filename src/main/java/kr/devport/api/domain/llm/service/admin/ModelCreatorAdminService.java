package kr.devport.api.domain.llm.service.admin;

import kr.devport.api.domain.llm.entity.ModelCreator;
import kr.devport.api.domain.llm.dto.request.admin.ModelCreatorCreateRequest;
import kr.devport.api.domain.llm.dto.request.admin.ModelCreatorUpdateRequest;
import kr.devport.api.domain.llm.dto.response.ModelCreatorResponse;
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
public class ModelCreatorAdminService {

    private final ModelCreatorRepository modelCreatorRepository;

    @CacheEvict(value = {
        CacheNames.LLM_MODELS,
        CacheNames.LLM_LEADERBOARD
    }, allEntries = true)
    public ModelCreatorResponse createModelCreator(ModelCreatorCreateRequest request) {
        ModelCreator creator = ModelCreator.builder()
            .externalId(request.getExternalId())
            .slug(request.getSlug())
            .name(request.getName())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        ModelCreator saved = modelCreatorRepository.save(creator);
        return convertToResponse(saved);
    }

    @CacheEvict(value = {
        CacheNames.LLM_MODELS,
        CacheNames.LLM_LEADERBOARD
    }, allEntries = true)
    public ModelCreatorResponse updateModelCreator(Long id, ModelCreatorUpdateRequest request) {
        ModelCreator creator = modelCreatorRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ModelCreator not found with id: " + id));

        if (request.getExternalId() != null) creator.setExternalId(request.getExternalId());
        if (request.getSlug() != null) creator.setSlug(request.getSlug());
        if (request.getName() != null) creator.setName(request.getName());

        creator.setUpdatedAt(LocalDateTime.now());
        ModelCreator updated = modelCreatorRepository.save(creator);
        return convertToResponse(updated);
    }

    @CacheEvict(value = {
        CacheNames.LLM_MODELS,
        CacheNames.LLM_LEADERBOARD
    }, allEntries = true)
    public void deleteModelCreator(Long id) {
        if (!modelCreatorRepository.existsById(id)) {
            throw new IllegalArgumentException("ModelCreator not found with id: " + id);
        }
        modelCreatorRepository.deleteById(id);
    }

    private ModelCreatorResponse convertToResponse(ModelCreator creator) {
        return ModelCreatorResponse.builder()
            .id(creator.getId())
            .externalId(creator.getExternalId())
            .slug(creator.getSlug())
            .name(creator.getName())
            .build();
    }
}
