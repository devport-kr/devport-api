package kr.devport.api.domain.common.exception;

/** 회원가입 이메일 인증번호/인증 토큰이 틀렸거나 만료됨 (400) */
public class EmailVerificationCodeException extends RuntimeException {
    public EmailVerificationCodeException(String message) {
        super(message);
    }
}
