package kr.devport.api.llm.service

import kr.devport.api.llm.dto.response.ImageEditingModelResponse
import kr.devport.api.llm.dto.response.ImageToVideoModelResponse
import kr.devport.api.llm.dto.response.TextToImageModelResponse
import kr.devport.api.llm.dto.response.TextToSpeechModelResponse
import kr.devport.api.llm.dto.response.TextToVideoModelResponse
import kr.devport.api.llm.repository.ImageEditingModelRepository
import kr.devport.api.llm.repository.ImageToVideoModelRepository
import kr.devport.api.llm.repository.TextToImageModelRepository
import kr.devport.api.llm.repository.TextToSpeechModelRepository
import kr.devport.api.llm.repository.TextToVideoModelRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LLMMediaService(
    private val textToImageModelRepository: TextToImageModelRepository,
    private val imageEditingModelRepository: ImageEditingModelRepository,
    private val textToSpeechModelRepository: TextToSpeechModelRepository,
    private val textToVideoModelRepository: TextToVideoModelRepository,
    private val imageToVideoModelRepository: ImageToVideoModelRepository,
) {
    fun getTextToImageModels(pageable: Pageable): Page<TextToImageModelResponse> =
        textToImageModelRepository.findAll(pageable).map { TextToImageModelResponse.fromEntity(it) }

    fun getImageEditingModels(pageable: Pageable): Page<ImageEditingModelResponse> =
        imageEditingModelRepository.findAll(pageable).map { ImageEditingModelResponse.fromEntity(it) }

    fun getTextToSpeechModels(pageable: Pageable): Page<TextToSpeechModelResponse> =
        textToSpeechModelRepository.findAll(pageable).map { TextToSpeechModelResponse.fromEntity(it) }

    fun getTextToVideoModels(pageable: Pageable): Page<TextToVideoModelResponse> =
        textToVideoModelRepository.findAll(pageable).map { TextToVideoModelResponse.fromEntity(it) }

    fun getImageToVideoModels(pageable: Pageable): Page<ImageToVideoModelResponse> =
        imageToVideoModelRepository.findAll(pageable).map { ImageToVideoModelResponse.fromEntity(it) }
}
