package com.cornercircle.backend.dashboard.service;

import com.cornercircle.backend.events.model.Event;
import com.cornercircle.backend.events.model.EventPublicationStatus;
import com.cornercircle.backend.events.repository.EventRepository;
import com.cornercircle.backend.membership.repository.MembershipApplicationRepository;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {
    @Test
    void reportsPublishedEventMissingDateAsNeedingDetails() {
        var members = mock(MembershipApplicationRepository.class);
        var registrations = mock(EventRegistrationRepository.class);
        var events = mock(EventRepository.class);
        var incomplete = new Event();
        incomplete.setPublicationStatus(EventPublicationStatus.PUBLISHED);
        incomplete.setEventDate(null);

        when(members.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of());
        when(registrations.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of());
        when(events.findUpcomingEvents(any(), any(LocalDate.class), any())).thenReturn(List.of());
        when(events.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(incomplete));

        var result = new AdminDashboardService(members, registrations, events).get();

        assertEquals(1, result.attentionItems().size());
        assertEquals("event-details", result.attentionItems().getFirst().id());
        assertEquals(1, result.attentionItems().getFirst().affectedCount());
    }

    @Test
    void doesNotReportIncompleteDraft() {
        var members = mock(MembershipApplicationRepository.class);
        var registrations = mock(EventRegistrationRepository.class);
        var events = mock(EventRepository.class);
        var draft = new Event();
        draft.setPublicationStatus(EventPublicationStatus.DRAFT);

        when(members.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of());
        when(registrations.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of());
        when(events.findUpcomingEvents(any(), any(LocalDate.class), any())).thenReturn(List.of());
        when(events.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(draft));

        var result = new AdminDashboardService(members, registrations, events).get();

        assertEquals(List.of(), result.attentionItems());
    }
}
