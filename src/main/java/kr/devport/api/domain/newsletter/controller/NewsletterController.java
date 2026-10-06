package kr.devport.api.domain.newsletter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.newsletter.dto.request.NewsletterSubscribeRequest;
import kr.devport.api.domain.newsletter.dto.request.NewsletterTokenRequest;
import kr.devport.api.domain.newsletter.dto.response.NewsletterActionResponse;
import kr.devport.api.domain.newsletter.dto.response.NewsletterSubscriptionResponse;
import kr.devport.api.domain.newsletter.service.NewsletterSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Tag(name = "Newsletter", description = "Newsletter subscription (email double opt-in) endpoints")
@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterSubscriptionService subscriptionService;

    @Value("${app.newsletter.site-url}")
    private String siteUrl;

    @Operation(summary = "Get my subscription", description = "Status is NONE, PENDING (waiting for email verification) or ACTIVE.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<NewsletterSubscriptionResponse> getMySubscription(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(userDetails.getId()));
    }

    @Operation(
        summary = "Subscribe (or resend verification / change email)",
        description = "Requires consent (agreed=true). Sends a verification link; the subscription becomes ACTIVE only after it is confirmed."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification email sent (or already ACTIVE with this email)"),
        @ApiResponse(responseCode = "400", description = "Invalid email or consent missing", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already subscribed by another account", content = @Content),
        @ApiResponse(responseCode = "429", description = "Verification email requested too often", content = @Content),
        @ApiResponse(responseCode = "503", description = "Email delivery failed", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/me")
    public ResponseEntity<NewsletterSubscriptionResponse> subscribe(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody NewsletterSubscribeRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.subscribe(userDetails.getId(), request));
    }

    @Operation(summary = "Unsubscribe", description = "Deletes the subscription (and the stored email).")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> unsubscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        subscriptionService.unsubscribe(userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "뉴스레터 구독이 해지되었습니다."));
    }

    @Operation(summary = "Confirm subscription", description = "Token from the verification email link. Does not require login.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription is now ACTIVE"),
        @ApiResponse(responseCode = "400", description = "Link expired", content = @Content),
        @ApiResponse(responseCode = "404", description = "Invalid or already used link", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already subscribed by another account", content = @Content)
    })
    @PostMapping("/confirm")
    public ResponseEntity<NewsletterActionResponse> confirm(@Valid @RequestBody NewsletterTokenRequest request) {
        return ResponseEntity.ok(subscriptionService.confirm(request.getToken()));
    }

    @Operation(summary = "Unsubscribe via email link", description = "Token from the unsubscribe link in every newsletter. Does not require login.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Unsubscribed"),
        @ApiResponse(responseCode = "404", description = "Already unsubscribed or invalid link", content = @Content)
    })
    @PostMapping("/unsubscribe")
    public ResponseEntity<NewsletterActionResponse> unsubscribeByToken(@Valid @RequestBody NewsletterTokenRequest request) {
        return ResponseEntity.ok(subscriptionService.unsubscribeByToken(request.getToken()));
    }

    @Operation(summary = "RFC 8058 one-click unsubscribe", description = "Called by mail clients (List-Unsubscribe-Post). Always 200.")
    @PostMapping("/unsubscribe/one-click")
    public ResponseEntity<Void> unsubscribeOneClick(@RequestParam String token) {
        subscriptionService.unsubscribeOneClick(token);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Open the unsubscribe page", description = "Mail clients that open the List-Unsubscribe URL in a browser are redirected to the web unsubscribe page.")
    @GetMapping("/unsubscribe/one-click")
    public ResponseEntity<Void> redirectToUnsubscribePage(@RequestParam String token) {
        String location = UriComponentsBuilder.fromUriString(siteUrl)
            .path("/newsletter/unsubscribe")
            .queryParam("token", token)
            .encode()
            .toUriString();
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location).build();
    }
}
