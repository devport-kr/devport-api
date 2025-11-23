package kr.devport.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.devport.api.domain.enums.BenchmarkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "benchmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Benchmark {

    @Id
    @Enumerated(EnumType.STRING)
    private BenchmarkType type;

    @Column(nullable = false, length = 100, name = "label_en")
    private String labelEn;  // "Agentic Coding"

    @Column(nullable = false, length = 100, name = "label_ko")
    private String labelKo;  // "에이전틱 코딩"

    @Column(columnDefinition = "TEXT", name = "description_en")
    private String descriptionEn;  // Full English description

    @Column(columnDefinition = "TEXT", name = "description_ko")
    private String descriptionKo;  // Full Korean description

    @Column(length = 10)
    private String icon;  // Emoji icon (e.g., "💻")
}
