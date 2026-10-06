package kr.devport.api.domain.newsletter.dto.request.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Send a preview of a newsletter issue to a single address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterTestSendRequest {

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must be less than 200 characters")
    private String subject;

    @NotBlank(message = "Content is required")
    @Size(max = 50000, message = "Content must be less than 50000 characters")
    private String content;

    @Schema(description = "Preview recipient", example = "admin@devport.kr")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
