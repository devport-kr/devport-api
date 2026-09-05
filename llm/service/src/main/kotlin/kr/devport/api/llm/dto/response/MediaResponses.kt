package kr.devport.api.llm.dto.response

import kr.devport.api.llm.entity.ImageEditingModel
import kr.devport.api.llm.entity.ImageToVideoCategory
import kr.devport.api.llm.entity.ImageToVideoModel
import kr.devport.api.llm.entity.TextToImageCategory
import kr.devport.api.llm.entity.TextToImageModel
import kr.devport.api.llm.entity.TextToSpeechModel
import kr.devport.api.llm.entity.TextToVideoCategory
import kr.devport.api.llm.entity.TextToVideoModel
import java.math.BigDecimal

data class TextToImageCategoryResponse(
    val styleCategory: String?,
    val subjectMatterCategory: String?,
    val elo: BigDecimal?,
    val ci95: String?,
    val appearances: Int?,
) {
    companion object {
        fun fromEntity(category: TextToImageCategory): TextToImageCategoryResponse =
            TextToImageCategoryResponse(
                category.styleCategory,
                category.subjectMatterCategory,
                category.elo,
                category.ci95,
                category.appearances,
            )
    }
}

data class TextToImageModelResponse(
    val id: Long?,
    val externalId: String?,
    val slug: String?,
    val name: String?,
    val modelCreator: ModelCreatorResponse?,
    val elo: BigDecimal?,
    val rank: Int?,
    val ci95: String?,
    val appearances: Int?,
    val releaseDate: String?,
    val categories: List<TextToImageCategoryResponse>,
) {
    companion object {
        fun fromEntity(model: TextToImageModel): TextToImageModelResponse =
            TextToImageModelResponse(
                id = model.id,
                externalId = model.externalId,
                slug = model.slug,
                name = model.name,
                modelCreator = ModelCreatorResponse.from(model.modelCreator),
                elo = model.elo,
                rank = model.rank,
                ci95 = model.ci95,
                appearances = model.appearances,
                releaseDate = model.releaseDate,
                categories = model.categories.map { TextToImageCategoryResponse.fromEntity(it) },
            )
    }
}

data class TextToVideoCategoryResponse(
    val styleCategory: String?,
    val subjectMatterCategory: String?,
    val formatCategory: String?,
    val elo: BigDecimal?,
    val ci95: String?,
    val appearances: Int?,
) {
    companion object {
        fun fromEntity(category: TextToVideoCategory): TextToVideoCategoryResponse =
            TextToVideoCategoryResponse(
                category.styleCategory,
                category.subjectMatterCategory,
                category.formatCategory,
                category.elo,
                category.ci95,
                category.appearances,
            )
    }
}

data class TextToVideoModelResponse(
    val id: Long?,
    val externalId: String?,
    val slug: String?,
    val name: String?,
    val modelCreator: ModelCreatorResponse?,
    val elo: BigDecimal?,
    val rank: Int?,
    val ci95: String?,
    val appearances: Int?,
    val releaseDate: String?,
    val categories: List<TextToVideoCategoryResponse>,
) {
    companion object {
        fun fromEntity(model: TextToVideoModel): TextToVideoModelResponse =
            TextToVideoModelResponse(
                id = model.id,
                externalId = model.externalId,
                slug = model.slug,
                name = model.name,
                modelCreator = ModelCreatorResponse.from(model.modelCreator),
                elo = model.elo,
                rank = model.rank,
                ci95 = model.ci95,
                appearances = model.appearances,
                releaseDate = model.releaseDate,
                categories = model.categories.map { TextToVideoCategoryResponse.fromEntity(it) },
            )
    }
}

data class ImageToVideoCategoryResponse(
    val styleCategory: String?,
    val subjectMatterCategory: String?,
    val formatCategory: String?,
    val elo: BigDecimal?,
    val ci95: String?,
    val appearances: Int?,
) {
    companion object {
        fun fromEntity(category: ImageToVideoCategory): ImageToVideoCategoryResponse =
            ImageToVideoCategoryResponse(
                category.styleCategory,
                category.subjectMatterCategory,
                category.formatCategory,
                category.elo,
                category.ci95,
                category.appearances,
            )
    }
}

data class ImageToVideoModelResponse(
    val id: Long?,
    val externalId: String?,
    val slug: String?,
    val name: String?,
    val modelCreator: ModelCreatorResponse?,
    val elo: BigDecimal?,
    val rank: Int?,
    val ci95: String?,
    val appearances: Int?,
    val releaseDate: String?,
    val categories: List<ImageToVideoCategoryResponse>,
) {
    companion object {
        fun fromEntity(model: ImageToVideoModel): ImageToVideoModelResponse =
            ImageToVideoModelResponse(
                id = model.id,
                externalId = model.externalId,
                slug = model.slug,
                name = model.name,
                modelCreator = ModelCreatorResponse.from(model.modelCreator),
                elo = model.elo,
                rank = model.rank,
                ci95 = model.ci95,
                appearances = model.appearances,
                releaseDate = model.releaseDate,
                categories = model.categories.map { ImageToVideoCategoryResponse.fromEntity(it) },
            )
    }
}

data class TextToSpeechModelResponse(
    val id: Long?,
    val externalId: String?,
    val slug: String?,
    val name: String?,
    val modelCreator: ModelCreatorResponse?,
    val elo: BigDecimal?,
    val rank: Int?,
    val ci95: String?,
    val appearances: Int?,
    val releaseDate: String?,
) {
    companion object {
        fun fromEntity(model: TextToSpeechModel): TextToSpeechModelResponse =
            TextToSpeechModelResponse(
                id = model.id,
                externalId = model.externalId,
                slug = model.slug,
                name = model.name,
                modelCreator = ModelCreatorResponse.from(model.modelCreator),
                elo = model.elo,
                rank = model.rank,
                ci95 = model.ci95,
                appearances = model.appearances,
                releaseDate = model.releaseDate,
            )
    }
}

data class ImageEditingModelResponse(
    val id: Long?,
    val externalId: String?,
    val slug: String?,
    val name: String?,
    val modelCreator: ModelCreatorResponse?,
    val elo: BigDecimal?,
    val rank: Int?,
    val ci95: String?,
    val appearances: Int?,
    val releaseDate: String?,
) {
    companion object {
        fun fromEntity(model: ImageEditingModel): ImageEditingModelResponse =
            ImageEditingModelResponse(
                id = model.id,
                externalId = model.externalId,
                slug = model.slug,
                name = model.name,
                modelCreator = ModelCreatorResponse.from(model.modelCreator),
                elo = model.elo,
                rank = model.rank,
                ci95 = model.ci95,
                appearances = model.appearances,
                releaseDate = model.releaseDate,
            )
    }
}
