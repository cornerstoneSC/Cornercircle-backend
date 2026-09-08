package com.cornercircle.backend.membership.payment;

import java.util.UUID;

public interface CheckoutGateway {
    CheckoutResult createAnnualMembershipCheckout(UUID applicationId, String customerEmail, String customerId, boolean renewal);
    String createBillingPortal(String customerId, UUID applicationId);
    record CheckoutResult(String sessionId, String url) {}
}
