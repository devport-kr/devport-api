package kr.devport.api.domain.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.enums.UserRole;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.logging.LogSanitizer;
import kr.devport.api.domain.common.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Value("${app.auth.current-terms-version}")
    private String currentTermsVersion;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // GitHub의 경우 기본 응답에 이메일이 없으면 별도 API로 조회한다.
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.debug("Loading OAuth2 user for provider={}", registrationId);

        if ("github".equals(registrationId)) {
            String currentEmail = oAuth2User.getAttribute("email");

            if (currentEmail == null) {
                String email = fetchGitHubEmail(userRequest.getAccessToken().getTokenValue());

                if (email != null) {
                    Map<String, Object> modifiedAttributes = new java.util.HashMap<>(oAuth2User.getAttributes());
                    modifiedAttributes.put("email", email);
                    oAuth2User = new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                        oAuth2User.getAuthorities(),
                        modifiedAttributes,
                        "id"
                    );
                }
            }
        }

        return processOAuth2User(userRequest, oAuth2User);
    }

    private String fetchGitHubEmail(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            log.debug("GitHub email lookup returned {} entries", emails != null ? emails.size() : 0);

            if (emails != null && !emails.isEmpty()) {
                for (Map<String, Object> emailData : emails) {
                    if (Boolean.TRUE.equals(emailData.get("primary"))) {
                        return (String) emailData.get("email");
                    }
                }
                return (String) emails.get(0).get("email");
            }
            log.debug("GitHub email lookup returned no usable email");
        } catch (Exception e) {
            log.warn("Failed to fetch GitHub email for OAuth2 login", e);
        }
        return null;
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
            registrationId,
            oAuth2User.getAttributes()
        );

        if (oAuth2UserInfo.getEmail() == null || oAuth2UserInfo.getEmail().isEmpty()) {
            log.warn("OAuth2 provider {} did not supply a usable email", registrationId);
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        AuthProvider authProvider = AuthProvider.valueOf(registrationId);
        Optional<User> userOptional = userRepository.findByAuthProviderAndProviderId(
            authProvider,
            oAuth2UserInfo.getId()
        );

        User user;
        if (userOptional.isPresent()) {
            user = updateExistingUser(userOptional.get(), oAuth2UserInfo);
        } else {
            // 같은 이메일로 이미 등록된 계정이 있는지 확인
            Optional<User> existingEmailUser = userRepository.findByEmail(oAuth2UserInfo.getEmail());
            if (existingEmailUser.isPresent()) {
                User existing = existingEmailUser.get();
                log.warn("OAuth2 registration conflict for provider={}, email={}, existingProvider={}",
                    authProvider,
                    LogSanitizer.maskEmail(oAuth2UserInfo.getEmail()),
                    existing.getAuthProvider());
                throw new OAuth2AuthenticationException("같은 정보의 계정이 이미 존재합니다");
            }

            // intent 검증: signup이 아니면 신규 가입 차단
            String intent = extractIntentFromCurrentRequest();
            if (!"signup".equals(intent)) {
                log.info("OAuth2 login blocked for unregistered user, provider={}, intent={}",
                    authProvider, intent);
                throw new OAuth2AuthenticationException("signup_required");
            }

            // 약관 동의 버전 검증
            String agreedTermsVersion = extractTermsVersionFromCurrentRequest();
            if (agreedTermsVersion == null || !currentTermsVersion.equals(agreedTermsVersion)) {
                log.warn("OAuth2 signup rejected: terms version mismatch (expected={}, got={})",
                    currentTermsVersion, agreedTermsVersion);
                throw new OAuth2AuthenticationException("약관 동의가 필요합니다");
            }

            user = registerNewUser(authProvider, oAuth2UserInfo);
            log.info("Registered new OAuth2 user with provider={}, userId={}", authProvider, user.getId());
        }

        return CustomUserDetails.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(AuthProvider authProvider, OAuth2UserInfo oAuth2UserInfo) {
        User user = User.builder()
            .email(oAuth2UserInfo.getEmail())
            .name(oAuth2UserInfo.getName())
            .profileImageUrl(oAuth2UserInfo.getImageUrl())
            .authProvider(authProvider)
            .providerId(oAuth2UserInfo.getId())
            .role(UserRole.USER)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .lastLoginAt(LocalDateTime.now())
            .agreedTermsVersion(currentTermsVersion)
            .agreedAt(LocalDateTime.now())
            .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        existingUser.setUpdatedAt(LocalDateTime.now());
        existingUser.setLastLoginAt(LocalDateTime.now());

        return userRepository.save(existingUser);
    }

    private String getStateFromCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getParameter("state");
    }

    private String extractIntentFromCurrentRequest() {
        return CustomOAuth2AuthorizationRequestResolver.extractIntentFromState(
                getStateFromCurrentRequest());
    }

    private String extractTermsVersionFromCurrentRequest() {
        return CustomOAuth2AuthorizationRequestResolver.extractTermsVersionFromState(
                getStateFromCurrentRequest());
    }
}
