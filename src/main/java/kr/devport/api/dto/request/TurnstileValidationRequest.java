package kr.devport.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Cloudflare Turnstile validation
 * Sent to Cloudflare's siteverify endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnstileValidationRequest {

    /**
     * The Cloudflare Turnstile secret key
     */
    private String secret;

    /**
     * The response token from Turnstile widget
     */
    private String response;

    /**
     * Optional: The user's IP address
     */
    private String remoteip;
}
