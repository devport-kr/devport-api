package kr.devport.api.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.devport.api.domain.enums.BenchmarkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** LLM 벤치마크 메타데이터 */
@Entity
@Table(name = "llm_benchmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMBenchmark {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "benchmark_type", length = 100)
    private BenchmarkType benchmarkType;

    @NotBlank
    @Column(nullable = false, length = 200, name = "display_name")
    private String displayName;

    @NotBlank
    @Column(nullable = false, length = 50, name = "category_group")
    private String categoryGroup;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @NotNull
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
