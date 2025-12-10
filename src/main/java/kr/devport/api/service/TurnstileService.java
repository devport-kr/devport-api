package kr.devport.api.service;

import kr.devport.api.dto.request.TurnstileValidationRequest;
import kr.devport.api.dto.response.TurnstileValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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

            ResponseEntity<TurnstileValidationResponse> response = restTemplate.exchange(
                TURNSTILE_VERIFY_URL,
                HttpMethod.POST,
                request,
                TurnstileValidationResponse.class
            );

            TurnstileValidationResponse validationResponse = response.getBody();

            if (validationResponse == null) {
                log.error("Turnstile validation failed: Response body is null");
                return false;
            }

            if (validationResponse.isSuccess()) {
                log.info("Turnstile token validated successfully for hostname: {}", validationResponse.getHostname());
            } else {
                log.warn("Turnstile token validation failed. Error codes: {}", validationResponse.getErrorCodes());
            }

            return validationResponse.isSuccess();

        } catch (Exception e) {
            log.error("Error during Turnstile token validation", e);
            return false;
        }
    }

    public boolean validateToken(String token) {
        return validateToken(token, null);
    }
}
