package kr.devport.api.domain.newsletter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.devport.api.domain.newsletter.entity.NewsletterSubscription;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "Current user's newsletter subscription")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewsletterSubscriptionResponse {

    public static final String STATUS_NONE = "NONE";

    @Schema(description = "NONE (not subscribed), PENDING (waiting for email verification) or ACTIVE",
        allowableValues = {"NONE", "PENDING", "ACTIVE"}, example = "ACTIVE")
    private String status;

    @Schema(description = "Subscribed email address", example = "user@example.com")
    private String email;

    @Schema(description = "When the user agreed to receive the newsletter")
    private LocalDateTime consentedAt;

    @Schema(description = "When the email address was verified (ACTIVE only)")
    private LocalDateTime verifiedAt;

    @Schema(description = "When the pending verification link expires (PENDING only)")
    private LocalDateTime verificationExpiresAt;

    public static NewsletterSubscriptionResponse none() {
        return NewsletterSubscriptionResponse.builder().status(STATUS_NONE).build();
    }

    public static NewsletterSubscriptionResponse from(NewsletterSubscription subscription) {
        boolean pending = subscription.getStatus() == NewsletterSubscriptionStatus.PENDING;
        return NewsletterSubscriptionResponse.builder()
            .status(subscription.getStatus().name())
            .email(subscription.getEmail())
            .consentedAt(subscription.getConsentedAt())
            .verifiedAt(subscription.getVerifiedAt())
            .verificationExpiresAt(pending ? subscription.getVerificationExpiresAt() : null)
            .build();
    }
}
