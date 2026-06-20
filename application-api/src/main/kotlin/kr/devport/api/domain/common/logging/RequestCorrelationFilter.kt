package kr.devport.api.domain.common.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestCorrelationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = LoggingContext.resolveRequestId(request.getHeader(LoggingContext.REQUEST_ID_HEADER))
        MDC.put(LoggingContext.REQUEST_ID_KEY, requestId)
        response.setHeader(LoggingContext.REQUEST_ID_HEADER, requestId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(LoggingContext.REQUEST_ID_KEY)
        }
    }
}
