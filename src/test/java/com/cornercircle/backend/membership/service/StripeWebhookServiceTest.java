package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.model.*;
import com.cornercircle.backend.membership.repository.*;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripeWebhookServiceTest {
    private final MembershipApplicationRepository applications = mock(MembershipApplicationRepository.class);
    private final ProcessedStripeEventRepository events = mock(ProcessedStripeEventRepository.class);
    private final EventRegistrationRepository eventRegistrations = mock(EventRegistrationRepository.class);
    private final StripeWebhookService service = new StripeWebhookService(applications, events, new ObjectMapper(), eventRegistrations);

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

    private static String payload(UUID id) {
        return "{\"data\":{\"object\":{\"id\":\"cs_123\",\"payment_status\":\"paid\",\"client_reference_id\":\"" + id + "\",\"metadata\":{\"membership_application_id\":\"" + id + "\"},\"customer\":\"cus_123\",\"payment_intent\":\"pi_123\"}}}";
    }
}
