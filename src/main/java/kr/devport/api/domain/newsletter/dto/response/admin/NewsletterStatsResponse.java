package kr.devport.api.domain.newsletter.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterStatsResponse {

    private long activeCount;
    private long pendingCount;
}
