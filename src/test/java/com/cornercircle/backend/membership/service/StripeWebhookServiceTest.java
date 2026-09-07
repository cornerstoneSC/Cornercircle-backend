package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.model.*;
import com.cornercircle.backend.membership.repository.*;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import com.cornercircle.backend.registration.service.EventConfirmationEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripeWebhookServiceTest {
    private final MembershipApplicationRepository applications = mock(MembershipApplicationRepository.class);
    private final ProcessedStripeEventRepository events = mock(ProcessedStripeEventRepository.class);
    private final EventRegistrationRepository eventRegistrations = mock(EventRegistrationRepository.class);
    private final EventConfirmationEmailService confirmationEmails = mock(EventConfirmationEmailService.class);
    private final MembershipEmailService membershipEmails = mock(MembershipEmailService.class);
    private final StripeWebhookService service = new StripeWebhookService(applications, events, new ObjectMapper(), eventRegistrations, confirmationEmails, membershipEmails);

    @Test void completedCheckoutActivatesMembershipAndStoresStripeReferences() throws Exception {
        UUID id = UUID.randomUUID();
        var application = new MembershipApplication(); application.setPublicId(id); application.setStatus(MembershipStatus.PENDING_PAYMENT);
        when(events.existsById("evt_1")).thenReturn(false);
        when(applications.findByPublicId(id)).thenReturn(Optional.of(application));
        service.process("evt_1", "checkout.session.completed", payload(id));
        assertEquals(MembershipStatus.ACTIVE, application.getStatus());
        assertEquals("cus_123", application.getStripeCustomerId());
        assertEquals("pi_123", application.getStripePaymentIntentId());
        assertNotNull(application.getPaidAt());
        assertNotNull(application.getMembershipStartsOn());
        assertEquals(application.getMembershipStartsOn().plusYears(1), application.getMembershipEndsOn());
        verify(membershipEmails).sendIfNeeded(application, false);
        verify(events).save(argThat(event -> event.getEventId().equals("evt_1")));
    }

    @Test void duplicateEventIsIgnored() throws Exception {
        when(events.existsById("evt_duplicate")).thenReturn(true);
        service.process("evt_duplicate", "checkout.session.completed", "{}");
        verifyNoInteractions(applications);
        verify(events, never()).save(any());
    }

    @Test void failedInvoiceMarksMembershipPastDue() throws Exception {
        var application = new MembershipApplication(); application.setStatus(MembershipStatus.ACTIVE);
        when(events.existsById("evt_failed")).thenReturn(false);
        when(applications.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.of(application));
        service.process("evt_failed", "invoice.payment_failed", "{\"data\":{\"object\":{\"subscription\":\"sub_123\"}}}");
        assertEquals(MembershipStatus.PAST_DUE, application.getStatus());
    }

    @Test void missingPaymentStatusNeverActivatesMembership() throws Exception {
        UUID id = UUID.randomUUID(); var application = new MembershipApplication();
        application.setPublicId(id); application.setStatus(MembershipStatus.PENDING_PAYMENT);
        when(applications.findByPublicId(id)).thenReturn(Optional.of(application));
        String payload = "{\"data\":{\"object\":{\"client_reference_id\":\"" + id + "\",\"metadata\":{\"membership_application_id\":\"" + id + "\"}}}}";
        service.process("evt_unpaid", "checkout.session.completed", payload);
        assertEquals(MembershipStatus.PENDING_PAYMENT, application.getStatus());
        verifyNoInteractions(membershipEmails);
    }

    @Test void renewalExtendsExistingEndDate() throws Exception {
        UUID id = UUID.randomUUID(); var application = new MembershipApplication();
        application.setPublicId(id); application.setStatus(MembershipStatus.ACTIVE);
        application.setMembershipStartsOn(java.time.LocalDate.now().minusMonths(6));
        application.setMembershipEndsOn(java.time.LocalDate.now().plusMonths(6));
        var oldEnd = application.getMembershipEndsOn();
        when(applications.findByPublicId(id)).thenReturn(Optional.of(application));
        String payload = "{\"data\":{\"object\":{\"payment_status\":\"paid\",\"client_reference_id\":\"" + id + "\",\"metadata\":{\"membership_application_id\":\"" + id + "\",\"membership_checkout_type\":\"renewal\"}}}}";
        service.process("evt_renewal", "checkout.session.completed", payload);
        assertEquals(oldEnd.plusYears(1), application.getMembershipEndsOn());
        verify(membershipEmails).sendIfNeeded(application, true);
    }

    private static String payload(UUID id) {
        return "{\"data\":{\"object\":{\"id\":\"cs_123\",\"payment_status\":\"paid\",\"amount_total\":19900,\"client_reference_id\":\"" + id + "\",\"metadata\":{\"membership_application_id\":\"" + id + "\"},\"customer\":\"cus_123\",\"payment_intent\":\"pi_123\"}}}";
    }
}
