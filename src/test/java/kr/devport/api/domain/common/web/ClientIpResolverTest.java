package kr.devport.api.domain.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void usesFirstForwardedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 130.176.1.1, 10.0.1.179");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("172.18.0.5");
    }

    @Test
    void classifiesPublicAndPrivateAddresses() {
        assertThat(ClientIpResolver.isPublicAddress("203.0.113.9")).isTrue();
        assertThat(ClientIpResolver.isPublicAddress("2001:db8::1")).isTrue();

        assertThat(ClientIpResolver.isPublicAddress("10.0.1.179")).isFalse();
        assertThat(ClientIpResolver.isPublicAddress("172.18.0.5")).isFalse();
        assertThat(ClientIpResolver.isPublicAddress("192.168.0.10")).isFalse();
        assertThat(ClientIpResolver.isPublicAddress("127.0.0.1")).isFalse();
        assertThat(ClientIpResolver.isPublicAddress("100.64.3.4")).isFalse();
        assertThat(ClientIpResolver.isPublicAddress("fd12:3456::1")).isFalse();
        assertThat(ClientIpResolver.isPublicAddress("::1")).isFalse();
        assertThat(ClientIpResolver.isPublicAddress("not-an-ip")).isFalse();
        assertThat(ClientIpResolver.isPublicAddress(null)).isFalse();
    }
}
