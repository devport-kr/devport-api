package kr.devport.api.domain.newsletter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Result of a token-based newsletter action (confirm / unsubscribe)")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewsletterActionResponse {

    @Schema(example = "뉴스레터 구독이 완료되었습니다.")
    private String message;

    @Schema(description = "Masked email address", example = "us***@example.com")
    private String email;
}
