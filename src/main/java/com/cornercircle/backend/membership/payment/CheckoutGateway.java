package com.cornercircle.backend.membership.payment;

import java.util.UUID;

public interface CheckoutGateway {
    CheckoutResult createAnnualMembershipCheckout(UUID applicationId, String customerEmail, boolean renewal);
    record CheckoutResult(String sessionId, String url) {}
}
