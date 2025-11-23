package kr.devport.api.dto.response;

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
public class LLMModelResponse {

    private Long id;
    private String name;
    private String provider;
    private Double score;
    private Integer rank;
    private String contextWindow;
    private String pricing;
}
