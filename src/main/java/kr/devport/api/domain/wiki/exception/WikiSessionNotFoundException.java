package kr.devport.api.domain.wiki.exception;

public class WikiSessionNotFoundException extends RuntimeException {
    public WikiSessionNotFoundException(String message) {
        super(message);
    }
}
