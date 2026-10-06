package kr.devport.api.domain.newsletter.dto.request.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Send the upcoming weekly digest to a single address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyDigestTestSendRequest {

    @Schema(description = "Preview recipient", example = "admin@devport.kr")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
