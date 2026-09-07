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
    private final MembershipApplicationRepository applications;
    private final CheckoutGateway checkout;

    public MembershipService(MembershipApplicationRepository applications, CheckoutGateway checkout) {
        this.applications = applications;
        this.checkout = checkout;
    }

    @Transactional
    public MembershipApplicationResponse apply(MembershipApplicationRequest request) {
        var application = new MembershipApplication();
        application.setPublicId(UUID.randomUUID());
        application.setFullName(clean(request.fullName()));
        application.setEmail(clean(request.email()).toLowerCase(Locale.ROOT));
        application.setPhone(cleanNullable(request.phone()));
        application.setCity(clean(request.city()));
        application.setBirthday(request.birthday());
        application.setInspiredBy(clean(request.inspiredBy()));
        application.setActivities(request.activities().stream().map(MembershipService::clean).distinct().toList());
        application.setGoals(request.goals().stream().map(MembershipService::clean).distinct().toList());
        application.setMembershipAgreementAccepted(request.membershipAgreementAccepted());
        application.setPhotographyNoticeAcknowledged(request.photographyNoticeAcknowledged());
        application.setComments(cleanNullable(request.comments()));
        application.setStatus(MembershipStatus.PENDING_PAYMENT);
        applications.save(application);
        return new MembershipApplicationResponse(application.getPublicId(), application.getStatus());
    }

    @Transactional
    public CheckoutSessionResponse checkout(UUID publicId) {
        var application = requireApplication(publicId);
        if (application.getStatus() == MembershipStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This membership is already active.");
        if (application.getStatus() == MembershipStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This membership application is cancelled.");
        var result = checkout.createAnnualMembershipCheckout(publicId, application.getEmail());
        application.setStripeCheckoutSessionId(result.sessionId());
        applications.save(application);
        return new CheckoutSessionResponse(result.url());
    }

    @Transactional(readOnly = true)
    public MembershipStatusResponse status(UUID publicId) {
        var application = requireApplication(publicId);
        return new MembershipStatusResponse(publicId, application.getStatus(), application.getStatus() == MembershipStatus.ACTIVE);
    }

    private MembershipApplication requireApplication(UUID publicId) {
        return applications.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership application not found."));
    }
    private static String clean(String value) { return value.trim().replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", ""); }
    private static String cleanNullable(String value) { return value == null || value.isBlank() ? null : clean(value); }
}
