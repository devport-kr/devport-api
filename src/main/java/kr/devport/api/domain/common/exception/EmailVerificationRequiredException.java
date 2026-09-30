package kr.devport.api.domain.common.exception;

public class EmailVerificationRequiredException extends RuntimeException {

    public EmailVerificationRequiredException(String message) {
        super(message);
    }
}
