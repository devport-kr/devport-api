package kr.devport.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "User signup request (username + password + verified email)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    @Schema(description = "Username (3-20 chars, alphanumeric + dash/underscore)", example = "johndoe")
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain alphanumeric characters, dash, and underscore")
    private String username;

    // BCrypt는 72바이트까지만 사용하므로 ASCII 64자로 제한한다.
    @Schema(description = "Password (8-64 ASCII chars, must contain at least one special character)", example = "Test@123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>])[\\x20-\\x7E]+$",
        message = "Password must contain at least one special character and only ASCII characters")
    private String password;

    @Schema(description = "Email address verified via /api/auth/signup/email-code/verify", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Schema(description = "verificationToken returned by /api/auth/signup/email-code/verify")
    @NotBlank(message = "Email verification is required")
    private String emailVerificationToken;

    @Schema(description = "Agreed terms version in YYYY-MM-DD format", example = "2026-03-24")
    @NotBlank(message = "Terms agreement is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Terms version must be in YYYY-MM-DD format")
    private String agreedTermsVersion;
}
