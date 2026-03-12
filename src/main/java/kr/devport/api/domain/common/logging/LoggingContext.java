package kr.devport.api.domain.common.logging;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

public final class LoggingContext {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_KEY = "requestId";

    private LoggingContext() {
    }

    public static String resolveRequestId(String requestIdHeader) {
        if (requestIdHeader != null && !requestIdHeader.isBlank()) {
            return requestIdHeader.strip();
        }
        return UUID.randomUUID().toString();
    }

    public static Runnable wrap(Runnable delegate) {
        Map<String, String> capturedContext = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previousContext = MDC.getCopyOfContextMap();
            try {
                if (capturedContext != null) {
                    MDC.setContextMap(capturedContext);
                } else {
                    MDC.clear();
                }
                delegate.run();
            } finally {
                if (previousContext != null) {
                    MDC.setContextMap(previousContext);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
