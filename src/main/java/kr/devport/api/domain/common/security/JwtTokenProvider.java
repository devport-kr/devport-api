package kr.devport.api.domain.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenProvider {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public String generateAccessToken(Long userId) {
        return generateToken(userId, accessTokenExpirationMs);
    }

    public String createAccessToken(Long userId) {
        return generateAccessToken(userId);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, refreshTokenExpirationMs);
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public Long getUserIdFromToken(String token) {
        Map<String, Object> claims = parseAndValidateClaims(token);
        Object subject = claims.get("sub");
        if (!(subject instanceof String subjectValue) || subjectValue.isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing");
        }
        return Long.parseLong(subjectValue);
    }

    public boolean validateToken(String token) {
        try {
            parseAndValidateClaims(token);
            return true;
        } catch (IllegalArgumentException ex) {
            log.debug("Rejected JWT token: {}", ex.getMessage());
        }
        return false;
    }

    private String generateToken(Long userId, long expirationMs) {
        long issuedAtSeconds = System.currentTimeMillis() / 1000;
        long expirationSeconds = issuedAtSeconds + (expirationMs / 1000);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS512");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("iat", issuedAtSeconds);
        payload.put("exp", expirationSeconds);

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signature = sign(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    private Map<String, Object> parseAndValidateClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token is empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token");
        }

        Map<String, Object> header = decodeJson(parts[0]);
        Object algorithm = header.get("alg");
        if (!"HS512".equals(algorithm)) {
            throw new IllegalArgumentException("Unsupported JWT token");
        }

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))
        ) {
            throw new IllegalArgumentException("Invalid JWT token");
        }

        Map<String, Object> claims = decodeJson(parts[1]);
        Object expiration = claims.get("exp");
        if (!(expiration instanceof Number expirationValue)) {
            throw new IllegalArgumentException("Invalid JWT token");
        }

        long nowSeconds = System.currentTimeMillis() / 1000;
        if (expirationValue.longValue() <= nowSeconds) {
            throw new IllegalArgumentException("Expired JWT token");
        }

        return claims;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(OBJECT_MAPPER.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize JWT payload", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeJson(String encodedValue) {
        try {
            byte[] decoded = BASE64_URL_DECODER.decode(encodedValue);
            return OBJECT_MAPPER.readValue(decoded, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec keySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            mac.init(keySpec);
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT token", e);
        }
    }
}
