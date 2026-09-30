package kr.devport.api.domain.auth.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlairUpdateRequest {

    @Size(max = 30, message = "Flair must be at most 30 characters")
    private String flair;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Flair color must be a valid hex color code (e.g., #a855f7)")
    private String flairColor;
}
