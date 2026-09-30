package kr.devport.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Profile update request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {

    @Schema(description = "Email address (optional)", example = "newemail@example.com")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Display name (optional)", example = "John Doe")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @Schema(description = "Profile image URL (optional)", example = "https://example.com/avatar.jpg")
    @Size(max = 500, message = "Profile image URL must be less than 500 characters")
    private String profileImageUrl;
}
