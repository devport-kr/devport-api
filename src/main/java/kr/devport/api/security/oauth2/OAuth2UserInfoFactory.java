package kr.devport.api.security.oauth2;

import kr.devport.api.domain.enums.AuthProvider;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.google.name())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.github.name())) {
            return new GitHubOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.naver.name())) {
            return new NaverOAuth2UserInfo(attributes);
        } else {
            throw new IllegalArgumentException("Login with " + registrationId + " is not supported");
        }
    }
}
