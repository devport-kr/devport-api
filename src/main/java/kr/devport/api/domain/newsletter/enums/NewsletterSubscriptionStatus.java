package kr.devport.api.domain.newsletter.enums;

public enum NewsletterSubscriptionStatus {
    /** 구독 신청 후 이메일 인증 대기 */
    PENDING,
    /** 이메일 인증 완료, 발송 대상 */
    ACTIVE
}
