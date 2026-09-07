package com.cornercircle.backend.registration.service;

import com.cornercircle.backend.events.repository.EventRepository;
import com.cornercircle.backend.registration.dto.EventRegistrationRequest;
import com.cornercircle.backend.registration.dto.EventRegistrationResponse;
import com.cornercircle.backend.registration.dto.EventRegistrationStatusResponse;
import com.cornercircle.backend.registration.model.EventRegistration;
import com.cornercircle.backend.registration.model.EventRegistrationStatus;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class EventRegistrationService {
    private static final String EVENT_POLICY_VERSION = "2026-09-06";
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final String secretKey;

    public EventRegistrationService(EventRepository events, EventRegistrationRepository registrations,
                                    @Value("${stripe.secret-key:}") String secretKey) {
        this.events = events;
        this.registrations = registrations;
        this.secretKey = secretKey;
    }

    @Transactional
    public EventRegistrationResponse prepare(String slug, EventRegistrationRequest request) {
        if (!Boolean.TRUE.equals(request.ageConfirmed()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must confirm that every attendee is at least 21 years old.");
        if (!Boolean.TRUE.equals(request.termsAccepted()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must accept the Event Terms and Cancellation Policy.");
        var event = events.findBySlugForRegistration(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));
        if (event.getPublicationStatus() != com.cornercircle.backend.events.model.EventPublicationStatus.PUBLISHED
                || event.getVisibility() != com.cornercircle.backend.events.model.EventVisibility.PUBLIC)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found.");
        ZoneId zone;
        try { zone = ZoneId.of(event.getTimeZone()); }
        catch (Exception ignored) { zone = ZoneId.of("America/Los_Angeles"); }
        ZonedDateTime now = ZonedDateTime.now(zone);
        if (event.getEventDate() == null || event.getEndTime() == null
                || event.getEventDate().atTime(event.getEndTime()).atZone(zone).isBefore(now))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This event has ended.");
        if (event.getRegistrationDeadline() != null && event.getRegistrationDeadline().atZone(zone).isBefore(now))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration has closed.");
        int quantity = request.guestCount();
        long reserved = registrations.activeReservedSeats(event.getId(), LocalDateTime.now().minusMinutes(20));
        if (event.getCapacity() != null && reserved + quantity > event.getCapacity())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough places remain.");
        var unitPrice = event.getPricePerPerson();
        if (unitPrice == null || unitPrice.signum() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This event has an invalid price.");
        var total = unitPrice.multiply(java.math.BigDecimal.valueOf(quantity));
        if (unitPrice.signum() == 0) {
            var registration = new EventRegistration(event, request.fullName(), request.email(), request.phone(), quantity, total, EventRegistrationStatus.CONFIRMED);
            registration.recordPolicyAcceptance(EVENT_POLICY_VERSION);
            registration = registrations.save(registration);
            return new EventRegistrationResponse(registration.getPublicId(), registration.getStatus().name(), null);
        }
        if (secretKey == null || secretKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe is not configured.");
        var registration = new EventRegistration(event, request.fullName(), request.email(), request.phone(), quantity, total, EventRegistrationStatus.PENDING_PAYMENT);
        registration.recordPolicyAcceptance(EVENT_POLICY_VERSION);
        registration = registrations.save(registration);
        try {
            Stripe.apiKey = secretKey;
            var params = PaymentIntentCreateParams.builder()
                    .setAmount(total.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact())
                    .setCurrency("usd")
                    .setReceiptEmail(request.email())
                    .setDescription(event.getTitle())
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .putMetadata("event_registration_id", registration.getPublicId().toString())
                    .putMetadata("event_slug", event.getSlug())
                    .putMetadata("quantity", String.valueOf(quantity))
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            registration.setStripePaymentIntentId(intent.getId());
            registrations.save(registration);
            return new EventRegistrationResponse(registration.getPublicId(), registration.getStatus().name(), intent.getClientSecret());
        } catch (StripeException exception) {
            registration.setStatus(EventRegistrationStatus.PAYMENT_FAILED);
            registrations.save(registration);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe could not prepare this payment.");
        }
    }

    @Transactional(readOnly = true)
    public EventRegistrationStatusResponse status(String slug, UUID registrationId) {
        var registration = registrations.findByPublicId(registrationId)
                .filter(item -> item.getEvent().getSlug().equals(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found."));
        return new EventRegistrationStatusResponse(registration.getPublicId(), registration.getStatus().name(), registration.getEmail());
    }
}
