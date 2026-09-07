package com.cornercircle.backend.registration.service;

import com.cornercircle.backend.events.model.Event;
import com.cornercircle.backend.registration.model.EventRegistration;
import com.cornercircle.backend.registration.model.EventRegistrationStatus;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventConfirmationEmailServiceTest {
    @Test
    void missingConfigurationNeverInvalidatesRegistration() {
        EventRegistrationRepository repository = mock(EventRegistrationRepository.class);
        Event event = new Event();
        event.setTitle("Coffee Social");
        event.setSlug("coffee-social");
        EventRegistration registration = new EventRegistration(event, "Ada Lovelace", "ada@example.com", "", 1, BigDecimal.ZERO, EventRegistrationStatus.CONFIRMED);
        var service = new EventConfirmationEmailService(repository, new ObjectMapper(), "", "", "", "http://localhost:3000");

        service.sendIfNeeded(registration);

        assertEquals(EventRegistrationStatus.CONFIRMED, registration.getStatus());
        assertNull(registration.getConfirmationEmailSentAt());
        assertEquals("Email delivery is not configured.", registration.getConfirmationEmailError());
        verify(repository).save(registration);
    }
}
