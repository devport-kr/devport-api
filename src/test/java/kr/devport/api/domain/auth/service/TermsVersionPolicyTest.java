package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.common.exception.InvalidTermsAgreementException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TermsVersionPolicyTest {

    @Test
    void acceptsOnlyCurrentVersionByDefault() {
        TermsVersionPolicy policy = new TermsVersionPolicy("2026-10-06", List.of(""));

        assertThat(policy.isAccepted("2026-10-06")).isTrue();
        assertThat(policy.isAccepted("2026-03-24")).isFalse();
        assertThat(policy.isAccepted(null)).isFalse();
    }

    @Test
    void acceptsConfiguredPreviousVersionsDuringRollout() {
        TermsVersionPolicy policy = new TermsVersionPolicy("2026-10-06", List.of(" 2026-03-24 "));

        assertThat(policy.isAccepted("2026-03-24")).isTrue();
        assertThatCode(() -> policy.requireAccepted("2026-03-24")).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireAccepted("2025-01-01"))
            .isInstanceOf(InvalidTermsAgreementException.class);
    }

    @Test
    void rejectsMalformedVersion() {
        TermsVersionPolicy policy = new TermsVersionPolicy("2026-10-06", List.of());

        assertThatThrownBy(() -> policy.requireAccepted("2026-13-40"))
            .isInstanceOf(InvalidTermsAgreementException.class)
            .hasMessageContaining("YYYY-MM-DD");
    }
}
