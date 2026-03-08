package kr.devport.api.domain.wiki.exception;

public class WikiChatRateLimitExceededException extends RuntimeException {
    public WikiChatRateLimitExceededException(String message) {
        super(message);
    }
}
