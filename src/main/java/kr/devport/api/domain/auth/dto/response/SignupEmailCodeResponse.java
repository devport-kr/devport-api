package kr.devport.api.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SignupEmailCodeResponse {
    private Long expiresIn; // 인증번호 유효 시간(초)
    private Long resendAvailableIn; // 다시 보낼 수 있을 때까지(초)
}
