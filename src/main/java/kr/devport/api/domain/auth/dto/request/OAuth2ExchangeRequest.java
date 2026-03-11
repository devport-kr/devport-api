package kr.devport.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "OAuth2 callback exchange request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2ExchangeRequest {

    @Schema(description = "One-time OAuth2 exchange code", example = "hPj6f6w8cG2wV7...")
    @NotBlank(message = "Code is required")
    private String code;
}
