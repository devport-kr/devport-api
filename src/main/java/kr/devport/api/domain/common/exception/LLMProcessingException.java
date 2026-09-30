package kr.devport.api.domain.common.exception;

public class LLMProcessingException extends RuntimeException {

    public LLMProcessingException(String message) {
        super(message);
    }

    public LLMProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
