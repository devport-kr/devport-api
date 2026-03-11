package kr.devport.api.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/** Cloudflare Turnstile 토큰을 검증 서비스. */
@Service
@RequiredArgsConstructor
@Slf4j
public class TurnstileService {

    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Value("${cloudflare.turnstile.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean validateToken(String token, String remoteIp) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Turnstile token validation failed: Token is null or empty");
            return false;
        }

        try {
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("secret", secretKey);
            requestBody.add("response", token);
            if (remoteIp != null && !remoteIp.isEmpty()) {
                requestBody.add("remoteip", remoteIp);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                TURNSTILE_VERIFY_URL,
                HttpMethod.POST,
                request,
                Map.class
            );

            Map<String, Object> validationResponse = response.getBody();

            if (validationResponse == null) {
                log.error("Turnstile validation failed: Response body is null");
                return false;
            }

            boolean success = Boolean.TRUE.equals(validationResponse.get("success"));
            Object hostname = validationResponse.get("hostname");
            Object errorCodes = validationResponse.get("error-codes");

            if (success) {
                log.info("Turnstile token validated successfully for hostname: {}", hostname);
            } else {
                log.warn("Turnstile token validation failed. Error codes: {}", errorCodes);
            }

            return success;

        } catch (Exception e) {
            log.error("Error during Turnstile token validation", e);
            return false;
        }
    }

    public boolean validateToken(String token) {
        return validateToken(token, null);
    }
}
