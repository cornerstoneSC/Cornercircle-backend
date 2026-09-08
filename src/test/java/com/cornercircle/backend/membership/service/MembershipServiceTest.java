package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.dto.MembershipApplicationRequest;
import com.cornercircle.backend.membership.model.*;
import com.cornercircle.backend.membership.payment.CheckoutGateway;
import com.cornercircle.backend.membership.repository.MembershipApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipServiceTest {
    private final MembershipApplicationRepository applications = mock(MembershipApplicationRepository.class);
    private final CheckoutGateway checkout = mock(CheckoutGateway.class);
    private final MembershipService service = new MembershipService(applications, checkout);

    @Test void storesApplicationAsPendingPayment() {
        var request = new MembershipApplicationRequest(" Gloria Example ", "GLORIA@example.com", null, "San Jose", null,
            "Connection", List.of("Coffee"), List.of("Friendship"), true, true, "Hello");
        var response = service.apply(request);
        assertNotNull(response.applicationId());
        assertEquals(MembershipStatus.PENDING_PAYMENT, response.status());
        verify(applications).save(argThat(saved -> saved.getEmail().equals("gloria@example.com") && saved.isMembershipAgreementAccepted()));
    }

    @Test void checkoutUsesStoredEmailAndOpaqueApplicationId() {
        UUID id = UUID.randomUUID();
        var application = pending(id);
        when(applications.findByPublicId(id)).thenReturn(Optional.of(application));
        when(checkout.createAnnualMembershipCheckout(id, "member@example.com", null, false))
            .thenReturn(new CheckoutGateway.CheckoutResult("cs_test_123", "https://checkout.stripe.com/test"));
        var response = service.checkout(id);
        assertEquals("https://checkout.stripe.com/test", response.checkoutUrl());
        assertEquals("cs_test_123", application.getStripeCheckoutSessionId());
    }

    @Test void activeMemberCannotStartAnotherCheckout() {
        UUID id = UUID.randomUUID();
        var application = pending(id); application.setStatus(MembershipStatus.ACTIVE);
        when(applications.findByPublicId(id)).thenReturn(Optional.of(application));
        assertThrows(ResponseStatusException.class, () -> service.checkout(id));
        verifyNoInteractions(checkout);
    }

    @Test void reusesPendingApplicationForSameEmail() {
        UUID id = UUID.randomUUID();
        var existing = pending(id);
        when(applications.findFirstByEmailAndStatusInOrderByCreatedAtDesc(eq("member@example.com"), any())).thenReturn(Optional.of(existing));
        var request = new MembershipApplicationRequest("Member", "MEMBER@example.com", null, "Oakland", null,
            "Friends", List.of("Coffee"), List.of("Community"), true, true, null);
        var response = service.apply(request);
        assertEquals(id, response.applicationId());
        verify(applications, never()).save(any());
    }

    @Test void marksPastMembershipExpiredWhenStatusIsChecked() {
        UUID id = UUID.randomUUID(); var application = pending(id);
        application.setStatus(MembershipStatus.ACTIVE); application.setMembershipEndsOn(java.time.LocalDate.now().minusDays(1));
        when(applications.findByPublicId(id)).thenReturn(Optional.of(application));
        var response = service.status(id);
        assertEquals(MembershipStatus.EXPIRED, response.status());
        assertFalse(response.paymentComplete());
    }

    private static MembershipApplication pending(UUID id) {
        var value = new MembershipApplication();
        value.setPublicId(id); value.setEmail("member@example.com"); value.setStatus(MembershipStatus.PENDING_PAYMENT);
        return value;
    }
}
