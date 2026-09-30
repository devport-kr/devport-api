package kr.devport.api.domain.llm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "llm_text_to_image_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextToImageCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_id")
    private TextToImageModel model;

    @Column(length = 100, name = "style_category")
    private String styleCategory;

    @Column(length = 100, name = "subject_matter_category")
    private String subjectMatterCategory;

    @Column(precision = 10, scale = 2)
    private BigDecimal elo;

    @Column(length = 20, name = "ci95")
    private String ci95;

    @Column(name = "appearances")
    private Integer appearances;
}
