package kr.devport.api.domain.common.exception

import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException
import kr.devport.api.domain.wiki.exception.WikiSessionNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateUsernameException::class)
    fun handleDuplicateUsername(ex: DuplicateUsernameException) = error(HttpStatus.CONFLICT, "Conflict", ex.message)

    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmail(ex: DuplicateEmailException) = error(HttpStatus.CONFLICT, "Conflict", ex.message)

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException) = error(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.message)

    @ExceptionHandler(EmailVerificationRequiredException::class)
    fun handleEmailVerificationRequired(ex: EmailVerificationRequiredException) = error(HttpStatus.FORBIDDEN, "Forbidden", ex.message)

    @ExceptionHandler(OAuth2AccountException::class)
    fun handleOAuth2Account(ex: OAuth2AccountException) = error(HttpStatus.BAD_REQUEST, "Bad Request", ex.message)

    @ExceptionHandler(InvalidTermsAgreementException::class)
    fun handleInvalidTermsAgreement(ex: InvalidTermsAgreementException) = error(HttpStatus.BAD_REQUEST, "Bad Request", ex.message)

    @ExceptionHandler(TokenExpiredException::class)
    fun handleTokenExpired(ex: TokenExpiredException) = error(HttpStatus.BAD_REQUEST, "Bad Request", ex.message)

    @ExceptionHandler(TokenNotFoundException::class)
    fun handleTokenNotFound(ex: TokenNotFoundException) = error(HttpStatus.NOT_FOUND, "Not Found", ex.message)

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(ex: InvalidTokenException) = error(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.message)

    @ExceptionHandler(UsernameNotFoundException::class)
    fun handleUsernameNotFound(ex: UsernameNotFoundException) = error(HttpStatus.NOT_FOUND, "Not Found", ex.message)

    @ExceptionHandler(LLMProcessingException::class)
    fun handleLLMProcessing(ex: LLMProcessingException) = error(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.message)

    @ExceptionHandler(WikiSessionNotFoundException::class)
    fun handleWikiSessionNotFound(ex: WikiSessionNotFoundException) = error(HttpStatus.NOT_FOUND, "Not Found", ex.message)

    @ExceptionHandler(WikiChatRateLimitExceededException::class)
    fun handleWikiChatRateLimit(ex: WikiChatRateLimitExceededException) =
        error(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", ex.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = HashMap<String, String?>()
        ex.bindingResult.allErrors.forEach { err ->
            val fieldName = (err as FieldError).field
            errors[fieldName] = err.defaultMessage
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                timestamp = LocalDateTime.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Validation Failed",
                message = "Invalid input data",
                validationErrors = errors,
            ),
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled API exception", ex)
        return error(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "서비스 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
        )
    }

    private fun error(
        status: HttpStatus,
        error: String,
        message: String?,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(timestamp = LocalDateTime.now(), status = status.value(), error = error, message = message),
        )

    data class ErrorResponse(
        val timestamp: LocalDateTime? = null,
        val status: Int = 0,
        val error: String? = null,
        val message: String? = null,
        val validationErrors: Map<String, String?>? = null,
    )

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
