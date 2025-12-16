package kr.devport.api.security.oauth2;

import java.util.Map;

/**
 * Naver OAuth2 사용자 정보 추출 클래스.
 *
 * Naver API 응답 구조:
 * {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "xxx",
 *     "email": "user@example.com",
 *     "name": "홍길동",
 *     "profile_image": "https://..."
 *   }
 * }
 */
public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Map<String, Object> response = getResponse();
        if (response == null) {
            return null;
        }
        Object id = response.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getName() {
        Map<String, Object> response = getResponse();
        if (response == null) {
            return null;
        }
        return (String) response.get("name");
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = getResponse();
        if (response == null) {
            return null;
        }
        return (String) response.get("email");
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> response = getResponse();
        if (response == null) {
            return null;
        }
        return (String) response.get("profile_image");
    }

    /**
     * Naver는 사용자 정보를 "response" 키 안에 중첩하여 반환한다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getResponse() {
        return (Map<String, Object>) attributes.get("response");
    }
}
