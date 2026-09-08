package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.model.*;
import com.cornercircle.backend.membership.repository.*;
import com.cornercircle.backend.registration.model.EventRegistrationStatus;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import com.cornercircle.backend.registration.service.EventConfirmationEmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
public class StripeWebhookService {
    private final MembershipApplicationRepository applications;
    private final ProcessedStripeEventRepository events;
    private final ObjectMapper mapper;
    private final EventRegistrationRepository eventRegistrations;
    private final EventConfirmationEmailService confirmationEmails;
    private final MembershipEmailService membershipEmails;

    public StripeWebhookService(MembershipApplicationRepository applications, ProcessedStripeEventRepository events, ObjectMapper mapper, EventRegistrationRepository eventRegistrations, EventConfirmationEmailService confirmationEmails, MembershipEmailService membershipEmails) {
        this.applications = applications;
        this.events = events;
        this.mapper = mapper;
        this.eventRegistrations = eventRegistrations;
        this.confirmationEmails = confirmationEmails;
        this.membershipEmails = membershipEmails;
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
            case "charge.refunded" -> {
                // Stripe also emits charge.refunded for partial refunds. Only a
                // fully refunded charge should end the membership.
                if (object.path("refunded").asBoolean(false))
                    findByStripeReferences(object).ifPresent(application -> application.setStatus(MembershipStatus.REFUNDED));
            }
            case "invoice.paid" -> invoicePaid(object);
            case "invoice.payment_failed" -> findByStripeReferences(object).ifPresent(application -> {
                application.setStatus(MembershipStatus.PAST_DUE);
                application.setStripeSubscriptionStatus("past_due");
                application.setLastStripeInvoiceId(text(object, "id"));
            });
            case "customer.subscription.updated" -> updateSubscription(object);
            case "customer.subscription.deleted" -> subscriptionDeleted(object);
            default -> { }
        }
        events.save(new ProcessedStripeEvent(eventId, eventType));
    }

    private void updateEventRegistration(JsonNode object, EventRegistrationStatus status) {
        String reference = text(object, "metadata", "event_registration_id");
        if (reference != null) {
            try { eventRegistrations.findByPublicId(UUID.fromString(reference)).ifPresent(registration -> { registration.setStatus(status); if (status == EventRegistrationStatus.CONFIRMED) confirmationEmails.sendIfNeeded(registration); }); }
            catch (IllegalArgumentException ignored) { }
            return;
        }
        String paymentIntentId = text(object, "id");
        if (paymentIntentId != null) eventRegistrations.findByStripePaymentIntentId(paymentIntentId).ifPresent(registration -> { registration.setStatus(status); if (status == EventRegistrationStatus.CONFIRMED) confirmationEmails.sendIfNeeded(registration); });
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
        if ("paid".equals(paymentStatus) || "no_payment_required".equals(paymentStatus)) {
            boolean renewal = "renewal".equals(text(object, "metadata", "membership_checkout_type"));
            Number amount = object.path("amount_total").isNumber() ? object.path("amount_total").numberValue() : null;
            if (amount != null) application.setAmountPaidCents(amount.intValue());
            application.setStripeCheckoutUrl(null); application.setCheckoutCreatedAt(null);
            if (renewal) renew(application); else activate(application);
            if (application.getStripeSubscriptionId() != null) application.setStripeSubscriptionStatus("active");
            membershipEmails.sendIfNeeded(application, renewal);
        }
    }

    private void invoicePaid(JsonNode object) {
        findByStripeReferences(object).ifPresent(application -> {
            boolean renewal = "subscription_cycle".equals(text(object, "billing_reason"));
            application.setStatus(MembershipStatus.ACTIVE);
            application.setStripeSubscriptionStatus("active");
            application.setLastStripeInvoiceId(text(object, "id"));
            String subscriptionId = subscriptionId(object);
            if (subscriptionId != null) application.setStripeSubscriptionId(subscriptionId);
            String paymentIntentId = id(object.path("payment_intent"));
            if (paymentIntentId != null) application.setStripePaymentIntentId(paymentIntentId);
            Number amount = object.path("amount_paid").isNumber() ? object.path("amount_paid").numberValue() : null;
            if (amount != null) application.setAmountPaidCents(amount.intValue());
            var period = object.path("lines").path("data");
            JsonNode line = period.isArray() && !period.isEmpty() ? period.get(0).path("period") : object;
            LocalDate start = date(line.path("start").asLong(object.path("period_start").asLong(0)));
            LocalDate end = date(line.path("end").asLong(object.path("period_end").asLong(0)));
            if (start != null) application.setMembershipStartsOn(start);
            if (end != null) application.setMembershipEndsOn(end);
            application.setPaidAt(LocalDateTime.now());
            application.setRenewalReminderSentAt(null);
            if (renewal) application.resetWelcomeEmail();
            membershipEmails.sendIfNeeded(application, renewal);
        });
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
        application.get().setStripeSubscriptionStatus(stripeStatus);
        application.get().setSubscriptionCancelAtPeriodEnd(object.path("cancel_at_period_end").asBoolean(false));
        long cancelledAt = object.path("canceled_at").asLong(0);
        application.get().setSubscriptionCancelledAt(cancelledAt > 0 ? dateTime(cancelledAt) : null);
        LocalDate start = subscriptionPeriodDate(object, "current_period_start");
        LocalDate end = subscriptionPeriodDate(object, "current_period_end");
        if (start != null) application.get().setMembershipStartsOn(start);
        if (end != null) application.get().setMembershipEndsOn(end);
        if (status == MembershipStatus.CANCELLED && end != null && !end.isBefore(LocalDate.now())) status = MembershipStatus.ACTIVE;
        application.get().setStatus(status);
    }

    private void subscriptionDeleted(JsonNode object) {
        findByStripeReferences(object).ifPresent(application -> {
            application.setStripeSubscriptionStatus("canceled");
            application.setSubscriptionCancelAtPeriodEnd(true);
            long cancelledAt = object.path("canceled_at").asLong(0);
            application.setSubscriptionCancelledAt(cancelledAt > 0 ? dateTime(cancelledAt) : LocalDateTime.now());
            LocalDate end = subscriptionPeriodDate(object, "current_period_end");
            if (end != null) application.setMembershipEndsOn(end);
            if (application.getMembershipEndsOn() != null && !application.getMembershipEndsOn().isBefore(LocalDate.now()))
                application.setStatus(MembershipStatus.ACTIVE);
            else application.setStatus(MembershipStatus.CANCELLED);
        });
    }

    private Optional<MembershipApplication> findByStripeReferences(JsonNode object) {
        String paymentIntentId = id(object.path("payment_intent"));
        if (paymentIntentId == null && "payment_intent".equals(text(object, "object"))) paymentIntentId = text(object, "id");
        if (paymentIntentId != null) {
            var found = applications.findByStripePaymentIntentId(paymentIntentId);
            if (found.isPresent()) return found;
        }
        String subscriptionId = subscriptionId(object);
        if (subscriptionId == null && "subscription".equals(text(object, "object"))) subscriptionId = text(object, "id");
        if (subscriptionId != null) {
            var found = applications.findByStripeSubscriptionId(subscriptionId);
            if (found.isPresent()) return found;
        }
        String customerId = id(object.path("customer"));
        return customerId == null ? Optional.empty() : applications.findByStripeCustomerId(customerId);
    }

    private static String subscriptionId(JsonNode object) {
        String value = id(object.path("subscription"));
        if (value == null) value = id(object.path("parent").path("subscription_details").path("subscription"));
        if (value == null && "subscription".equals(text(object, "object"))) value = text(object, "id");
        return value;
    }

    private static LocalDate subscriptionPeriodDate(JsonNode object, String field) {
        long timestamp = object.path(field).asLong(0);
        if (timestamp == 0) {
            JsonNode items = object.path("items").path("data");
            if (items.isArray() && !items.isEmpty()) timestamp = items.get(0).path(field).asLong(0);
        }
        return date(timestamp);
    }

    private static LocalDate date(long epochSeconds) {
        return epochSeconds <= 0 ? null : Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static LocalDateTime dateTime(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    private static void activate(MembershipApplication application) {
        var today = LocalDate.now();
        application.setStatus(MembershipStatus.ACTIVE);
        if (application.getPaidAt() == null) application.setPaidAt(LocalDateTime.now());
        if (application.getMembershipStartsOn() == null) application.setMembershipStartsOn(today);
        if (application.getMembershipEndsOn() == null) application.setMembershipEndsOn(today.plusYears(1));
    }

    private static void renew(MembershipApplication application) {
        var today = LocalDate.now();
        var base = application.getMembershipEndsOn() != null && !application.getMembershipEndsOn().isBefore(today)
            ? application.getMembershipEndsOn() : today;
        application.setStatus(MembershipStatus.ACTIVE);
        application.setPaidAt(LocalDateTime.now());
        if (application.getMembershipStartsOn() == null) application.setMembershipStartsOn(today);
        application.setMembershipEndsOn(base.plusYears(1));
        application.setRenewalReminderSentAt(null);
        application.resetWelcomeEmail();
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
