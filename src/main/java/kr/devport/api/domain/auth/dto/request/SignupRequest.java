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

@Schema(description = "User signup request")
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

    @Schema(description = "Password (min 8 chars, must contain at least one special character)", example = "Test@123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).+$", message = "Password must contain at least one special character")
    private String password;

    @Schema(description = "Email address (required for LOCAL users)", example = "test@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Display name (optional)", example = "John Doe")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @Schema(description = "Agreed terms version in YYYY-MM-DD format", example = "2026-03-24")
    @NotBlank(message = "Terms agreement is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Terms version must be in YYYY-MM-DD format")
    private String agreedTermsVersion;
}
