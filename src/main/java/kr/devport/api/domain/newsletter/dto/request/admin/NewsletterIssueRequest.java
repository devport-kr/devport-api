package kr.devport.api.domain.newsletter.dto.request.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Newsletter issue to send to all active subscribers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterIssueRequest {

    @Schema(description = "Email subject", example = "이번 주 개발 트렌드")
    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must be less than 200 characters")
    private String subject;

    @Schema(description = "Plain-text body. URLs become links and line breaks are kept in the HTML version.")
    @NotBlank(message = "Content is required")
    @Size(max = 50000, message = "Content must be less than 50000 characters")
    private String content;
}
