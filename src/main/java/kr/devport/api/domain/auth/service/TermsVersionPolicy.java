package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.common.exception.InvalidTermsAgreementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 회원가입 시 동의한 약관 버전이 유효한지 판단한다.
 * 약관 개정 직후 백엔드/프론트 배포 시점이 어긋나도 가입이 끊기지 않도록
 * 직전 버전을 app.auth.accepted-previous-terms-versions 로 한시 허용할 수 있다.
 */
@Slf4j
@Component
public class TermsVersionPolicy {

    private final String currentVersion;
    private final List<String> acceptedPreviousVersions;

    public TermsVersionPolicy(
        @Value("${app.auth.current-terms-version}") String currentVersion,
        @Value("${app.auth.accepted-previous-terms-versions:}") List<String> acceptedPreviousVersions
    ) {
        this.currentVersion = currentVersion;
        this.acceptedPreviousVersions = acceptedPreviousVersions.stream()
            .map(String::trim)
            .filter(version -> !version.isEmpty())
            .toList();
    }

    public boolean isAccepted(String agreedVersion) {
        return agreedVersion != null
            && (currentVersion.equals(agreedVersion) || acceptedPreviousVersions.contains(agreedVersion));
    }

    public void requireAccepted(String agreedVersion) {
        try {
            LocalDate.parse(agreedVersion);
        } catch (Exception ex) {
            throw new InvalidTermsAgreementException("Terms version must be a valid date in YYYY-MM-DD format");
        }

        if (!isAccepted(agreedVersion)) {
            log.warn("Signup rejected: terms version mismatch (expected={}, got={})", currentVersion, agreedVersion);
            throw new InvalidTermsAgreementException("You must agree to the current terms version to sign up");
        }
    }

    public String getCurrentVersion() {
        return currentVersion;
    }
}
