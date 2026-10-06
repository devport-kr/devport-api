package kr.devport.api.domain.common.web;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /** 프록시(nginx) 뒤에서 X-Forwarded-For의 첫 번째 값을 클라이언트 IP로 사용한다. */
    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 실제 사용자 IP로 볼 수 있는 공인 주소인지 판단한다.
     * 프록시가 원래 IP를 넘기지 않으면 모든 요청이 같은 사설 IP(예: 10.0.1.x)로 보이므로,
     * 그런 주소로 IP 단위 rate limit을 걸면 사이트 전체가 한 한도를 나눠 쓰게 된다.
     */
    public static boolean isPublicAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.ofLiteral(ip.trim());
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                return false;
            }
            byte[] bytes = address.getAddress();
            boolean ipv6UniqueLocal = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;           // fc00::/7
            boolean carrierGradeNat = bytes.length == 4 && (bytes[0] & 0xff) == 100 && (bytes[1] & 0xc0) == 64; // 100.64.0.0/10
            return !ipv6UniqueLocal && !carrierGradeNat;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
