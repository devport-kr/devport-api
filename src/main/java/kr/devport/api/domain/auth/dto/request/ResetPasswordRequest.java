package kr.devport.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Reset password request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @Schema(description = "Reset token from email", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Token is required")
    private String token;

    @Schema(description = "New password (min 8 chars, must contain at least one special character)", example = "NewPass@456")
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).+$", message = "Password must contain at least one special character")
    private String newPassword;
}
