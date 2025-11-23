package kr.devport.api.dto.response;

import kr.devport.api.domain.enums.BenchmarkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchmarkResponse {

    private BenchmarkType type;
    private String labelEn;
    private String labelKo;
    private String descriptionEn;
    private String descriptionKo;
    private String icon;
}
