package kr.devport.api.domain.common.exception;

public class OAuth2AccountException extends RuntimeException {
    public OAuth2AccountException(String message) {
        super(message);
    }
}
