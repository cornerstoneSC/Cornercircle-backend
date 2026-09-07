package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.model.*;
import com.cornercircle.backend.membership.repository.*;
import com.cornercircle.backend.registration.model.EventRegistrationStatus;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class StripeWebhookService {
    private final MembershipApplicationRepository applications;
    private final ProcessedStripeEventRepository events;
    private final ObjectMapper mapper;
    private final EventRegistrationRepository eventRegistrations;

    public StripeWebhookService(MembershipApplicationRepository applications, ProcessedStripeEventRepository events, ObjectMapper mapper, EventRegistrationRepository eventRegistrations) {
        this.applications = applications;
        this.events = events;
        this.mapper = mapper;
        this.eventRegistrations = eventRegistrations;
    }

    @Transactional
    public void process(String eventId, String eventType, String payload) throws Exception {
        if (events.existsById(eventId)) return;
        JsonNode object = mapper.readTree(payload).path("data").path("object");
        switch (eventType) {
            case "checkout.session.completed" -> completeCheckout(object);
            case "checkout.session.async_payment_succeeded" -> completeCheckout(object);
            case "checkout.session.async_payment_failed" -> updateByApplication(object, MembershipStatus.PAYMENT_FAILED);
            case "payment_intent.succeeded" -> updateEventRegistration(object, EventRegistrationStatus.CONFIRMED);
            case "payment_intent.payment_failed" -> {
                updateByApplication(object, MembershipStatus.PAYMENT_FAILED);
                updateEventRegistration(object, EventRegistrationStatus.PAYMENT_FAILED);
            }
            case "charge.refunded" -> findByStripeReferences(object).ifPresent(application -> application.setStatus(MembershipStatus.REFUNDED));
            // Legacy subscription events remain supported for existing records.
            case "invoice.paid" -> findByStripeReferences(object).ifPresent(application -> application.setStatus(MembershipStatus.ACTIVE));
            case "invoice.payment_failed" -> findByStripeReferences(object).ifPresent(application -> application.setStatus(MembershipStatus.PAST_DUE));
            case "customer.subscription.updated" -> updateSubscription(object);
            case "customer.subscription.deleted" -> findByStripeReferences(object).ifPresent(application -> application.setStatus(MembershipStatus.CANCELLED));
            default -> { }
        }
        events.save(new ProcessedStripeEvent(eventId, eventType));
    }

    private void updateEventRegistration(JsonNode object, EventRegistrationStatus status) {
        String reference = text(object, "metadata", "event_registration_id");
        if (reference != null) {
            try { eventRegistrations.findByPublicId(UUID.fromString(reference)).ifPresent(registration -> registration.setStatus(status)); }
            catch (IllegalArgumentException ignored) { }
            return;
        }
        String paymentIntentId = text(object, "id");
        if (paymentIntentId != null) eventRegistrations.findByStripePaymentIntentId(paymentIntentId).ifPresent(registration -> registration.setStatus(status));
    }

    private void completeCheckout(JsonNode object) {
        String reference = text(object, "metadata", "membership_application_id");
        if (reference == null) reference = text(object, "client_reference_id");
        if (reference == null) return;
        MembershipApplication application;
        try { application = applications.findByPublicId(UUID.fromString(reference)).orElse(null); }
        catch (IllegalArgumentException ignored) { return; }
        if (application == null) return;
        application.setStripeCheckoutSessionId(text(object, "id"));
        application.setStripeCustomerId(id(object.path("customer")));
        application.setStripeSubscriptionId(id(object.path("subscription")));
        application.setStripePaymentIntentId(id(object.path("payment_intent")));
        String paymentStatus = text(object, "payment_status");
        if (paymentStatus == null || "paid".equals(paymentStatus) || "no_payment_required".equals(paymentStatus)) activate(application);
    }

    private void updateByApplication(JsonNode object, MembershipStatus status) {
        String reference = text(object, "metadata", "membership_application_id");
        if (reference == null) reference = text(object, "client_reference_id");
        if (reference == null) return;
        try { applications.findByPublicId(UUID.fromString(reference)).ifPresent(application -> application.setStatus(status)); }
        catch (IllegalArgumentException ignored) { }
    }

    private void updateSubscription(JsonNode object) {
        var application = findByStripeReferences(object);
        if (application.isEmpty()) return;
        String stripeStatus = text(object, "status");
        MembershipStatus status = switch (stripeStatus == null ? "" : stripeStatus) {
            case "active", "trialing" -> MembershipStatus.ACTIVE;
            case "past_due", "unpaid", "incomplete", "incomplete_expired" -> MembershipStatus.PAST_DUE;
            case "canceled", "paused" -> MembershipStatus.CANCELLED;
            default -> application.get().getStatus();
        };
        application.get().setStatus(status);
    }

    private Optional<MembershipApplication> findByStripeReferences(JsonNode object) {
        String paymentIntentId = id(object.path("payment_intent"));
        if (paymentIntentId == null && "payment_intent".equals(text(object, "object"))) paymentIntentId = text(object, "id");
        if (paymentIntentId != null) {
            var found = applications.findByStripePaymentIntentId(paymentIntentId);
            if (found.isPresent()) return found;
        }
        String subscriptionId = id(object.path("subscription"));
        if (subscriptionId == null) subscriptionId = id(object.path("parent").path("subscription_details").path("subscription"));
        if (subscriptionId == null && "subscription".equals(text(object, "object"))) subscriptionId = text(object, "id");
        if (subscriptionId != null) {
            var found = applications.findByStripeSubscriptionId(subscriptionId);
            if (found.isPresent()) return found;
        }
        String customerId = id(object.path("customer"));
        return customerId == null ? Optional.empty() : applications.findByStripeCustomerId(customerId);
    }

    private static void activate(MembershipApplication application) {
        var today = LocalDate.now();
        application.setStatus(MembershipStatus.ACTIVE);
        if (application.getPaidAt() == null) application.setPaidAt(LocalDateTime.now());
        if (application.getMembershipStartsOn() == null) application.setMembershipStartsOn(today);
        if (application.getMembershipEndsOn() == null) application.setMembershipEndsOn(today.plusYears(1));
    }

    private static String id(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        return node.path("id").isTextual() ? node.path("id").asText() : null;
    }
    private static String text(JsonNode node, String... path) {
        JsonNode current = node;
        for (String part : path) current = current.path(part);
        return current.isTextual() && !current.asText().isBlank() ? current.asText() : null;
    }
}
