package kr.devport.api.domain.newsletter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Newsletter subscription request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterSubscribeRequest {

    @Schema(description = "Email address to receive the newsletter", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must be less than 100 characters")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Invalid email format")
    private String email;

    @Schema(description = "Agreement to collect the email and receive the newsletter (must be true)", example = "true")
    @NotNull(message = "Consent is required")
    @AssertTrue(message = "You must agree to receive the newsletter")
    private Boolean agreed;

    @Schema(description = "Cloudflare Turnstile token (required for every verification email)")
    @NotBlank(message = "Bot verification is required")
    private String turnstileToken;
}
