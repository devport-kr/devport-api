package kr.devport.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO from Cloudflare Turnstile validation
 * Received from Cloudflare's siteverify endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnstileValidationResponse {

    /**
     * Whether the token is valid
     */
    private boolean success;

    /**
     * Timestamp when the challenge was solved
     */
    @JsonProperty("challenge_ts")
    private String challengeTs;

    /**
     * Hostname for which the token was generated
     */
    private String hostname;

    /**
     * List of error codes (if validation failed)
     * Possible values:
     * - missing-input-secret: The secret parameter was not passed.
     * - invalid-input-secret: The secret parameter was invalid or did not exist.
     * - missing-input-response: The response parameter (token) was not passed.
     * - invalid-input-response: The response parameter (token) is invalid or has expired.
     * - bad-request: The request was rejected because it was malformed.
     * - timeout-or-duplicate: The response parameter has already been validated before.
     */
    @JsonProperty("error-codes")
    private List<String> errorCodes;

    /**
     * Optional: Action name for Managed Challenge widgets
     */
    private String action;

    /**
     * Optional: Customer data passed through
     */
    private String cdata;
}
