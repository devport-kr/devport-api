package kr.devport.api.domain.llm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "llm_text_to_image_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextToImageModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100, name = "external_id")
    private String externalId;

    @Column(unique = true, length = 200)
    private String slug;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_creator_id")
    private ModelCreator modelCreator;

    @Column(precision = 10, scale = 2)
    private BigDecimal elo;

    @Column(name = "model_rank")
    private Integer rank;

    @Column(length = 20, name = "ci95")
    private String ci95;

    @Column(name = "appearances")
    private Integer appearances;

    @Column(length = 20, name = "release_date")
    private String releaseDate;

    @Default
    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TextToImageCategory> categories = new ArrayList<>();

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
