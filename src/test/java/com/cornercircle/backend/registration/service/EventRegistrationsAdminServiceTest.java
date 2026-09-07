package com.cornercircle.backend.registration.service;

import com.cornercircle.backend.events.model.Event;
import com.cornercircle.backend.registration.model.EventRegistration;
import com.cornercircle.backend.registration.model.EventRegistrationStatus;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventRegistrationsAdminServiceTest {
    private EventRegistrationRepository repository;
    private TicketTokenService tickets;
    private EventRegistrationsAdminService service;
    private EventConfirmationEmailService confirmationEmails;
    private EventRegistration registration;

    @BeforeEach
    void setUp() {
        repository = mock(EventRegistrationRepository.class);
        tickets = new TicketTokenService("abcdefghijklmnopqrstuvwxyz123456");
        confirmationEmails = mock(EventConfirmationEmailService.class);
        service = new EventRegistrationsAdminService(repository, tickets, confirmationEmails);
        Event event = new Event();
        event.setTitle("Coffee Social");
        event.setSlug("coffee-social");
        registration = new EventRegistration(event, "Ada Lovelace", "ada@example.com", "", 2, BigDecimal.TEN, EventRegistrationStatus.CONFIRMED);
        when(repository.findByPublicId(registration.getPublicId())).thenReturn(Optional.of(registration));
    }

    @Test
    void validatesAndChecksInTicketOnlyOnce() {
        String token = tickets.issue(registration.getPublicId());
        assertEquals("VALID", service.validate(token).status());
        assertEquals("CHECKED_IN", service.checkIn(token).status());
        assertNotNull(registration.getCheckedInAt());
        assertEquals("ALREADY_CHECKED_IN", service.checkIn(token).status());
        verify(repository, times(1)).save(registration);
    }

    @Test
    void rejectsForgedTicket() {
        assertThrows(ResponseStatusException.class, () -> service.validate(registration.getPublicId() + ".forged"));
        verify(repository, never()).findByPublicId(any());
    }
}
