package kr.devport.api.domain.wiki.exception

class WikiChatRateLimitExceededException(
    message: String,
) : RuntimeException(message)

class WikiSessionNotFoundException(
    message: String,
) : RuntimeException(message)
