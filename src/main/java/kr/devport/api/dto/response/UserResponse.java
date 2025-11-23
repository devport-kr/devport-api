package kr.devport.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.devport.api.domain.enums.AuthProvider;
import kr.devport.api.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "User information response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @Schema(description = "User display name", example = "John Doe")
    private String name;

    @Schema(description = "Profile image URL", example = "https://avatars.githubusercontent.com/u/12345")
    private String profileImageUrl;

    @Schema(description = "OAuth provider", example = "GITHUB")
    private AuthProvider authProvider;

    @Schema(description = "User role", example = "USER")
    private UserRole role;

    @Schema(description = "Account creation timestamp", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last login timestamp", example = "2025-01-15T14:30:00")
    private LocalDateTime lastLoginAt;
}
