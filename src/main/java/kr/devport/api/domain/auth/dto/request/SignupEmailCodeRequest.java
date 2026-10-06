package kr.devport.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Request a signup email verification code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupEmailCodeRequest {

    @Schema(description = "Email address to verify", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Schema(description = "Cloudflare Turnstile token")
    @NotBlank(message = "Bot verification is required")
    private String turnstileToken;
}
