package com.cornercircle.backend.membership.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Component
public class StripeCheckoutGateway implements CheckoutGateway {
    private final String secretKey;
    private final String priceId;
    private final String frontendUrl;

    public StripeCheckoutGateway(@Value("${stripe.secret-key:}") String secretKey,
                                 @Value("${stripe.membership-price-id:}") String priceId,
                                 @Value("${stripe.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.secretKey = secretKey;
        this.priceId = priceId;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public CheckoutResult createAnnualMembershipCheckout(UUID applicationId, String customerEmail, String customerId, boolean renewal) {
        requireConfigured();
        var metadataValue = applicationId.toString();
        var builder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setClientReferenceId(metadataValue)
            .setSuccessUrl(frontendUrl + "/membership/success?application_id=" + metadataValue + "&session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(frontendUrl + "/membership?payment=cancelled")
            .setAllowPromotionCodes(true)
            .putMetadata("membership_application_id", metadataValue)
            .putMetadata("membership_checkout_type", renewal ? "renewal" : "initial")
            .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                .putMetadata("membership_application_id", metadataValue)
                .putMetadata("membership_checkout_type", renewal ? "renewal" : "initial").build())
            .addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build());
        if (customerId != null && customerId.startsWith("cus_")) builder.setCustomer(customerId);
        else builder.setCustomerEmail(customerEmail);
        try {
            Session session = Session.create(builder.build(), RequestOptions.builder().setApiKey(secretKey).build());
            if (session.getUrl() == null || session.getId() == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe did not return a Checkout URL.");
            return new CheckoutResult(session.getId(), session.getUrl());
        } catch (StripeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to start secure checkout. Please try again.", exception);
        }
    }

    @Override
    public String createBillingPortal(String customerId, UUID applicationId) {
        requireConfigured();
        if (customerId == null || !customerId.startsWith("cus_"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Billing management is not available for this legacy membership yet.");
        try {
            var params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(customerId)
                .setReturnUrl(frontendUrl + "/membership/success?application_id=" + applicationId)
                .build();
            var session = com.stripe.model.billingportal.Session.create(params, RequestOptions.builder().setApiKey(secretKey).build());
            if (session.getUrl() == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe did not return a billing portal URL.");
            return session.getUrl();
        } catch (StripeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to open billing management. Please try again.", exception);
        }
    }

    private void requireConfigured() {
        if (!secretKey.startsWith("sk_") || !priceId.startsWith("price_"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe membership checkout is not configured.");
    }
}
