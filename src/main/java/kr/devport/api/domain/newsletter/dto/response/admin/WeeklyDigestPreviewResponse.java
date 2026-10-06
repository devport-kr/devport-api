package kr.devport.api.domain.newsletter.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** 다음 주간 다이제스트에 지금까지 선정된 글 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyDigestPreviewResponse {

    private String digestWeek;
    /** 후보 기간 (KST) */
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private String subject;
    private int candidateCount;
    private List<WeeklyDigestArticleResponse> articles;
}
