package kr.devport.api.dto.response;

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
    private String summaryKoTitle;
    private String url;
    private LocalDateTime createdAtSource;
}
