package kr.devport.api.dto.response;

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
public class AuthResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private UserResponse user;

    public AuthResponse(String accessToken, UserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.user = user;
    }
}
