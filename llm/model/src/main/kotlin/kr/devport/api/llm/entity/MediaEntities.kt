package kr.devport.api.llm.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "llm_text_to_image_models")
class TextToImageModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, length = 100, name = "external_id")
    var externalId: String? = null

    @Column(unique = true, length = 200)
    var slug: String? = null

    @Column(nullable = false, length = 200)
    var name: String = ""

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_creator_id")
    var modelCreator: ModelCreator? = null

    @Column(precision = 10, scale = 2)
    var elo: BigDecimal? = null

    @Column(name = "model_rank")
    var rank: Int? = null

    @Column(length = 20, name = "ci95")
    var ci95: String? = null

    @Column(name = "appearances")
    var appearances: Int? = null

    @Column(length = 20, name = "release_date")
    var releaseDate: String? = null

    @OneToMany(mappedBy = "model", cascade = [CascadeType.ALL], orphanRemoval = true)
    var categories: MutableList<TextToImageCategory> = mutableListOf()

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

@Entity
@Table(name = "llm_text_to_image_categories")
class TextToImageCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_id")
    var model: TextToImageModel? = null

    @Column(length = 100, name = "style_category")
    var styleCategory: String? = null

    @Column(length = 100, name = "subject_matter_category")
    var subjectMatterCategory: String? = null

    @Column(precision = 10, scale = 2)
    var elo: BigDecimal? = null

    @Column(length = 20, name = "ci95")
    var ci95: String? = null

    @Column(name = "appearances")
    var appearances: Int? = null
}

@Entity
@Table(name = "llm_text_to_video_models")
class TextToVideoModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, length = 100, name = "external_id")
    var externalId: String? = null

    @Column(unique = true, length = 200)
    var slug: String? = null

    @Column(nullable = false, length = 200)
    var name: String = ""

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_creator_id")
    var modelCreator: ModelCreator? = null

    @Column(precision = 10, scale = 2)
    var elo: BigDecimal? = null

    @Column(name = "model_rank")
    var rank: Int? = null

    @Column(length = 20, name = "ci95")
    var ci95: String? = null

    @Column(name = "appearances")
    var appearances: Int? = null

    @Column(length = 20, name = "release_date")
    var releaseDate: String? = null

    @OneToMany(mappedBy = "model", cascade = [CascadeType.ALL], orphanRemoval = true)
    var categories: MutableList<TextToVideoCategory> = mutableListOf()

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

@Entity
@Table(name = "llm_text_to_video_categories")
class TextToVideoCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_id")
    var model: TextToVideoModel? = null

    @Column(length = 100, name = "style_category")
    var styleCategory: String? = null

    @Column(length = 100, name = "subject_matter_category")
    var subjectMatterCategory: String? = null

    @Column(length = 100, name = "format_category")
    var formatCategory: String? = null

    @Column(precision = 10, scale = 2)
    var elo: BigDecimal? = null

    @Column(length = 20, name = "ci95")
    var ci95: String? = null

    @Column(name = "appearances")
    var appearances: Int? = null
}

@Entity
@Table(name = "llm_image_to_video_models")
class ImageToVideoModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, length = 100, name = "external_id")
    var externalId: String? = null

    @Column(unique = true, length = 200)
    var slug: String? = null

    @Column(nullable = false, length = 200)
    var name: String = ""

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_creator_id")
    var modelCreator: ModelCreator? = null

    @Column(precision = 10, scale = 2)
    var elo: BigDecimal? = null

    @Column(name = "model_rank")
    var rank: Int? = null

    @Column(length = 20, name = "ci95")
    var ci95: String? = null

    @Column(name = "appearances")
    var appearances: Int? = null

    @Column(length = 20, name = "release_date")
    var releaseDate: String? = null

    @OneToMany(mappedBy = "model", cascade = [CascadeType.ALL], orphanRemoval = true)
    var categories: MutableList<ImageToVideoCategory> = mutableListOf()

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

@Entity
@Table(name = "llm_image_to_video_categories")
class ImageToVideoCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_id")
    var model: ImageToVideoModel? = null

    @Column(length = 100, name = "style_category")
    var styleCategory: String? = null

    @Column(length = 100, name = "subject_matter_category")
    var subjectMatterCategory: String? = null

    @Column(length = 100, name = "format_category")
    var formatCategory: String? = null

    @Column(precision = 10, scale = 2)
    var elo: BigDecimal? = null

    @Column(length = 20, name = "ci95")
    var ci95: String? = null

    @Column(name = "appearances")
    var appearances: Int? = null
}

@Entity
@Table(name = "llm_text_to_speech_models")
class TextToSpeechModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, length = 100, name = "external_id")
    var externalId: String? = null

    @Column(unique = true, length = 200)
    var slug: String? = null

    @Column(nullable = false, length = 200)
    var name: String = ""

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_creator_id")
    var modelCreator: ModelCreator? = null

    @Column(precision = 10, scale = 2)
    var elo: BigDecimal? = null

    @Column(name = "model_rank")
    var rank: Int? = null

    @Column(length = 20, name = "ci95")
    var ci95: String? = null

    @Column(name = "appearances")
    var appearances: Int? = null

    @Column(length = 20, name = "release_date")
    var releaseDate: String? = null

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

@Entity
@Table(name = "llm_image_editing_models")
class ImageEditingModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, length = 100, name = "external_id")
    var externalId: String? = null

    @Column(unique = true, length = 200)
    var slug: String? = null

    @Column(nullable = false, length = 200)
    var name: String = ""

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_creator_id")
    var modelCreator: ModelCreator? = null

    @Column(precision = 10, scale = 2)
    var elo: BigDecimal? = null

    @Column(name = "model_rank")
    var rank: Int? = null

    @Column(length = 20, name = "ci95")
    var ci95: String? = null

    @Column(name = "appearances")
    var appearances: Int? = null

    @Column(length = 20, name = "release_date")
    var releaseDate: String? = null

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
