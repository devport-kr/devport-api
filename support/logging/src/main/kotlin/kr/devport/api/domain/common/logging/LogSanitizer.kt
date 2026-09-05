package kr.devport.api.domain.common.logging

object LogSanitizer {
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) {
            return "unknown"
        }

        val atIndex = email.indexOf('@')
        if (atIndex <= 0 || atIndex == email.length - 1) {
            return "***"
        }

        val local = email.substring(0, atIndex)
        val domain = email.substring(atIndex + 1)
        val maskedLocal = if (local.length <= 2) "${local[0]}*" else local.substring(0, 2) + "***"
        return "$maskedLocal@$domain"
    }

    fun maskIp(ip: String?): String {
        if (ip.isNullOrBlank()) {
            return "unknown"
        }

        if (ip.contains(".")) {
            val parts = ip.split(".")
            if (parts.size == 4) {
                return "${parts[0]}.${parts[1]}.*.*"
            }
        }

        if (ip.contains(":")) {
            val lastColon = ip.lastIndexOf(':')
            if (lastColon > 0) {
                return ip.substring(0, lastColon) + ":*"
            }
        }

        return "***"
    }
}
