package com.cornercircle.backend.registration.service;

import com.cornercircle.backend.events.model.Event;
import com.cornercircle.backend.events.model.EventPublicationStatus;
import com.cornercircle.backend.events.model.EventVisibility;
import com.cornercircle.backend.events.repository.EventRepository;
import com.cornercircle.backend.registration.dto.EventRegistrationRequest;
import com.cornercircle.backend.registration.model.EventRegistration;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventRegistrationServiceTest {
    private EventRepository events;
    private EventRegistrationRepository registrations;
    private EventRegistrationService service;

    @BeforeEach
    void setUp() {
        events = mock(EventRepository.class);
        registrations = mock(EventRegistrationRepository.class);
        service = new EventRegistrationService(events, registrations, "", new TicketTokenService("abcdefghijklmnopqrstuvwxyz123456"), "http://localhost:3000");
        when(registrations.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void confirmsFreeRegistrationWithoutStripe() {
        Event event = openEvent();
        when(events.findBySlugForRegistration("coffee-social")).thenReturn(Optional.of(event));
        when(registrations.activeReservedSeats(isNull(), any())).thenReturn(0L);

        var response = service.prepare("coffee-social", new EventRegistrationRequest("Ada Lovelace", "ada@example.com", "", 2, true, true));

        assertEquals("CONFIRMED", response.status());
        assertNull(response.clientSecret());
        verify(registrations).save(any(EventRegistration.class));
    }

    @Test
    void rejectsHiddenEventRegistration() {
        Event event = openEvent();
        event.setVisibility(EventVisibility.HIDDEN);
        when(events.findBySlugForRegistration("coffee-social")).thenReturn(Optional.of(event));

        assertThrows(ResponseStatusException.class, () -> service.prepare(
                "coffee-social", new EventRegistrationRequest("Ada Lovelace", "ada@example.com", "", 1, true, true)));
        verifyNoInteractions(registrations);
    }

    @Test
    void rejectsRegistrationBeyondRemainingCapacity() {
        Event event = openEvent();
        when(events.findBySlugForRegistration("coffee-social")).thenReturn(Optional.of(event));
        when(registrations.activeReservedSeats(isNull(), any())).thenReturn(9L);

        assertThrows(ResponseStatusException.class, () -> service.prepare(
                "coffee-social", new EventRegistrationRequest("Ada Lovelace", "ada@example.com", "", 2, true, true)));
    }

    @Test
    void rejectsRegistrationWithoutAgeConfirmation() {
        assertThrows(ResponseStatusException.class, () -> service.prepare(
                "coffee-social", new EventRegistrationRequest("Ada Lovelace", "ada@example.com", "", 1, false, true)));
        verifyNoInteractions(events, registrations);
    }

    @Test
    void rejectsRegistrationWithoutPolicyAcceptance() {
        assertThrows(ResponseStatusException.class, () -> service.prepare(
                "coffee-social", new EventRegistrationRequest("Ada Lovelace", "ada@example.com", "", 1, true, false)));
        verifyNoInteractions(events, registrations);
    }

    private Event openEvent() {
        Event event = new Event();
        event.setTitle("Coffee Social");
        event.setSlug("coffee-social");
        event.setPublicationStatus(EventPublicationStatus.PUBLISHED);
        event.setVisibility(EventVisibility.PUBLIC);
        event.setEventDate(LocalDate.now().plusDays(7));
        event.setStartTime(LocalTime.of(10, 0));
        event.setEndTime(LocalTime.of(12, 0));
        event.setTimeZone("America/Los_Angeles");
        event.setCapacity(10);
        event.setPricePerPerson(BigDecimal.ZERO);
        return event;
    }
}
