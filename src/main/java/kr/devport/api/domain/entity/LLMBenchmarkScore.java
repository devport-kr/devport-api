package kr.devport.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.devport.api.domain.enums.BenchmarkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "llm_benchmark_scores",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"model_id", "benchmark_type"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMBenchmarkScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private LLMModel model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "benchmark_type")
    private BenchmarkType benchmarkType;

    @Column(nullable = false)
    private Double score;  // Percentage score (0-100)

    @Column(nullable = false)
    private Integer rank;  // Rank position for this benchmark

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;  // When benchmark score was last updated
}
