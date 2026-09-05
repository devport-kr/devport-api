package kr.devport.api.domain.common.logging

import org.slf4j.MDC
import java.util.UUID

object LoggingContext {
    const val REQUEST_ID_HEADER = "X-Request-ID"
    const val REQUEST_ID_KEY = "requestId"

    fun resolveRequestId(requestIdHeader: String?): String {
        if (!requestIdHeader.isNullOrBlank()) {
            return requestIdHeader.trim()
        }
        return UUID.randomUUID().toString()
    }

    fun wrap(delegate: Runnable): Runnable {
        val capturedContext: Map<String, String>? = MDC.getCopyOfContextMap()
        return Runnable {
            val previousContext: Map<String, String>? = MDC.getCopyOfContextMap()
            try {
                if (capturedContext != null) {
                    MDC.setContextMap(capturedContext)
                } else {
                    MDC.clear()
                }
                delegate.run()
            } finally {
                if (previousContext != null) {
                    MDC.setContextMap(previousContext)
                } else {
                    MDC.clear()
                }
            }
        }
    }
}
