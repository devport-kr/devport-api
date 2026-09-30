package kr.devport.api.domain.llm.service;

import kr.devport.api.domain.llm.dto.response.*;
import kr.devport.api.domain.llm.repository.ImageEditingModelRepository;
import kr.devport.api.domain.llm.repository.ImageToVideoModelRepository;
import kr.devport.api.domain.llm.repository.TextToImageModelRepository;
import kr.devport.api.domain.llm.repository.TextToSpeechModelRepository;
import kr.devport.api.domain.llm.repository.TextToVideoModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LLMMediaService {

    private final TextToImageModelRepository textToImageModelRepository;
    private final ImageEditingModelRepository imageEditingModelRepository;
    private final TextToSpeechModelRepository textToSpeechModelRepository;
    private final TextToVideoModelRepository textToVideoModelRepository;
    private final ImageToVideoModelRepository imageToVideoModelRepository;

    public Page<TextToImageModelResponse> getTextToImageModels(Pageable pageable) {
        return textToImageModelRepository.findAll(pageable)
            .map(TextToImageModelResponse::fromEntity);
    }

    public Page<ImageEditingModelResponse> getImageEditingModels(Pageable pageable) {
        return imageEditingModelRepository.findAll(pageable)
            .map(ImageEditingModelResponse::fromEntity);
    }

    public Page<TextToSpeechModelResponse> getTextToSpeechModels(Pageable pageable) {
        return textToSpeechModelRepository.findAll(pageable)
            .map(TextToSpeechModelResponse::fromEntity);
    }

    public Page<TextToVideoModelResponse> getTextToVideoModels(Pageable pageable) {
        return textToVideoModelRepository.findAll(pageable)
            .map(TextToVideoModelResponse::fromEntity);
    }

    public Page<ImageToVideoModelResponse> getImageToVideoModels(Pageable pageable) {
        return imageToVideoModelRepository.findAll(pageable)
            .map(ImageToVideoModelResponse::fromEntity);
    }
}
