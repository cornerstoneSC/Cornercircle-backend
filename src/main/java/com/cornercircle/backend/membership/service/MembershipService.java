package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.dto.*;
import com.cornercircle.backend.membership.model.*;
import com.cornercircle.backend.membership.payment.CheckoutGateway;
import com.cornercircle.backend.membership.repository.MembershipApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import java.util.UUID;

@Service
public class MembershipService {
    private static final String AGREEMENT_VERSION = "2026-09-07";
    private static final String PHOTOGRAPHY_VERSION = "2026-09-07";
    private final MembershipApplicationRepository applications;
    private final CheckoutGateway checkout;

    public MembershipService(MembershipApplicationRepository applications, CheckoutGateway checkout) {
        this.applications = applications;
        this.checkout = checkout;
    }

    @Transactional
    public MembershipApplicationResponse apply(MembershipApplicationRequest request) {
        String email = clean(request.email()).toLowerCase(Locale.ROOT);
        var existing = applications.findFirstByEmailAndStatusInOrderByCreatedAtDesc(email,
            java.util.List.of(MembershipStatus.PENDING_PAYMENT, MembershipStatus.ACTIVE));
        if (existing.isPresent()) {
            var application = existing.get();
            if (application.getStatus() == MembershipStatus.ACTIVE &&
                (application.getMembershipEndsOn() == null || !application.getMembershipEndsOn().isBefore(java.time.LocalDate.now())))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An active membership already exists for this email address.");
            if (application.getStatus() == MembershipStatus.ACTIVE) {
                application.setStatus(MembershipStatus.EXPIRED);
                return new MembershipApplicationResponse(application.getPublicId(), application.getStatus());
            }
            if (application.getStatus() == MembershipStatus.PENDING_PAYMENT)
                return new MembershipApplicationResponse(application.getPublicId(), application.getStatus());
        }
        var application = new MembershipApplication();
        application.setPublicId(UUID.randomUUID());
        application.setFullName(clean(request.fullName()));
        application.setEmail(email);
        application.setPhone(cleanNullable(request.phone()));
        application.setCity(clean(request.city()));
        application.setBirthday(request.birthday());
        application.setInspiredBy(clean(request.inspiredBy()));
        application.setActivities(request.activities().stream().map(MembershipService::clean).distinct().toList());
        application.setGoals(request.goals().stream().map(MembershipService::clean).distinct().toList());
        application.recordConsent(AGREEMENT_VERSION, PHOTOGRAPHY_VERSION);
        application.setComments(cleanNullable(request.comments()));
        application.setStatus(MembershipStatus.PENDING_PAYMENT);
        applications.save(application);
        return new MembershipApplicationResponse(application.getPublicId(), application.getStatus());
    }

    @Transactional
    public CheckoutSessionResponse checkout(UUID publicId) {
        var application = requireApplication(publicId);
        if (application.getStatus() == MembershipStatus.ACTIVE && application.getMembershipEndsOn() != null
            && application.getMembershipEndsOn().isBefore(java.time.LocalDate.now())) application.setStatus(MembershipStatus.EXPIRED);
        if (application.getStatus() == MembershipStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This membership is already active.");
        if (application.getStatus() == MembershipStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This membership application is cancelled.");
        if (application.getStatus() == MembershipStatus.REFUNDED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This membership was refunded. Submit a new membership application to join again.");
        if (application.getStripeCheckoutUrl() != null && application.getCheckoutCreatedAt() != null
            && application.getCheckoutCreatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(30)))
            return new CheckoutSessionResponse(application.getStripeCheckoutUrl());
        boolean renewal = application.getStatus() == MembershipStatus.EXPIRED;
        var result = checkout.createAnnualMembershipCheckout(publicId, application.getEmail(), renewal);
        application.setStripeCheckoutSessionId(result.sessionId());
        application.setStripeCheckoutUrl(result.url());
        application.setCheckoutCreatedAt(java.time.LocalDateTime.now());
        applications.save(application);
        return new CheckoutSessionResponse(result.url());
    }

    @Transactional
    public CheckoutSessionResponse renewalCheckout(UUID publicId) {
        var application = requireApplication(publicId);
        if (application.getStatus() != MembershipStatus.ACTIVE && application.getStatus() != MembershipStatus.EXPIRED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This membership is not eligible for renewal.");
        if (application.getStripeCheckoutUrl() != null && application.getCheckoutCreatedAt() != null
            && application.getCheckoutCreatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(30)))
            return new CheckoutSessionResponse(application.getStripeCheckoutUrl());
        var result = checkout.createAnnualMembershipCheckout(publicId, application.getEmail(), true);
        application.setStripeCheckoutSessionId(result.sessionId());
        application.setStripeCheckoutUrl(result.url());
        application.setCheckoutCreatedAt(java.time.LocalDateTime.now());
        applications.save(application);
        return new CheckoutSessionResponse(result.url());
    }

    @Transactional
    public MembershipStatusResponse status(UUID publicId) {
        var application = requireApplication(publicId);
        if (application.getStatus() == MembershipStatus.ACTIVE && application.getMembershipEndsOn() != null
            && application.getMembershipEndsOn().isBefore(java.time.LocalDate.now())) application.setStatus(MembershipStatus.EXPIRED);
        return new MembershipStatusResponse(publicId, application.getStatus(), application.getStatus() == MembershipStatus.ACTIVE);
    }

    private MembershipApplication requireApplication(UUID publicId) {
        return applications.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership application not found."));
    }
    private static String clean(String value) { return value.trim().replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", ""); }
    private static String cleanNullable(String value) { return value == null || value.isBlank() ? null : clean(value); }
}
