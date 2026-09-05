package kr.devport.api.domain.common.exception

class DuplicateEmailException(
    message: String,
) : RuntimeException(message)

class DuplicateUsernameException(
    message: String,
) : RuntimeException(message)

class EmailVerificationRequiredException(
    message: String,
) : RuntimeException(message)

class InvalidCredentialsException(
    message: String,
) : RuntimeException(message)

class InvalidTermsAgreementException(
    message: String,
) : RuntimeException(message)

class InvalidTokenException(
    message: String,
) : RuntimeException(message)

class LLMProcessingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class OAuth2AccountException(
    message: String,
) : RuntimeException(message)

class TokenExpiredException(
    message: String,
) : RuntimeException(message)

class TokenNotFoundException(
    message: String,
) : RuntimeException(message)
