package com.cornercircle.backend.membership.model;

public enum MembershipStatus {
    PENDING_PAYMENT,
    ACTIVE,
    PAYMENT_FAILED,
    REFUNDED,
    EXPIRED,
    // Kept for compatibility with memberships created before one-time payments.
    PAST_DUE,
    CANCELLED
}
