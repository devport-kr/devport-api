package kr.devport.api.security.oauth2;

import kr.devport.api.domain.entity.User;
import kr.devport.api.domain.enums.AuthProvider;
import kr.devport.api.domain.enums.UserRole;
import kr.devport.api.repository.UserRepository;
import kr.devport.api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // GitHub의 경우 기본 응답에 이메일이 없으면 별도 API로 조회한다.
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if ("github".equals(registrationId) && oAuth2User.getAttribute("email") == null) {
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
            if (emails != null && !emails.isEmpty()) {
                for (Map<String, Object> emailData : emails) {
                    if (Boolean.TRUE.equals(emailData.get("primary"))) {
                        return (String) emailData.get("email");
                    }
                }
                return (String) emails.get(0).get("email");
            }
        } catch (Exception e) {
            // 조회 실패 시에도 인증 흐름은 중단하지 않는다.
            e.printStackTrace();
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
                throw new OAuth2AuthenticationException("같은 정보의 계정이 이미 존재합니다");
            }
            user = registerNewUser(authProvider, oAuth2UserInfo);
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
}
