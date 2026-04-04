package kr.devport.api.domain.article.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingTickerResponse {

    private Long id;
    private String externalId;
    private String summaryKoTitle;
    private LocalDateTime createdAtSource;
}
