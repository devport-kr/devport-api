package kr.devport.api.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SignupEmailVerificationResponse {
    private String verificationToken; // 회원가입 요청에 emailVerificationToken으로 보낸다
    private Long expiresIn; // 토큰 유효 시간(초)
}
