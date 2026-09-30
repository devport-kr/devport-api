package kr.devport.api.domain.common.logging;

public final class LogSanitizer {

    private LogSanitizer() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return "***";
        }

        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        String maskedLocal = local.length() <= 2
                ? local.charAt(0) + "*"
                : local.substring(0, 2) + "***";
        return maskedLocal + "@" + domain;
    }

    public static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }

        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }

        if (ip.contains(":")) {
            int lastColon = ip.lastIndexOf(':');
            if (lastColon > 0) {
                return ip.substring(0, lastColon) + ":*";
            }
        }

        return "***";
    }
}
