package com.cornercircle.backend.registration.service;

import com.cornercircle.backend.registration.dto.*;
import com.cornercircle.backend.registration.model.EventRegistration;
import com.cornercircle.backend.registration.model.EventRegistrationStatus;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class EventRegistrationsAdminService {
    private final EventRegistrationRepository registrations;
    private final TicketTokenService tickets;
    private final EventConfirmationEmailService confirmationEmails;

    public EventRegistrationsAdminService(EventRegistrationRepository registrations, TicketTokenService tickets, EventConfirmationEmailService confirmationEmails) {
        this.registrations = registrations;
        this.tickets = tickets;
        this.confirmationEmails = confirmationEmails;
    }

    @Transactional(readOnly = true)
    public AdminEventRegistrationsResponse list(String query, String eventSlug) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String event = eventSlug == null ? "" : eventSlug.trim();
        var paid = registrations.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .filter(registration -> registration.getStatus() == EventRegistrationStatus.CONFIRMED)
            .toList();
        var visible = paid.stream()
            .filter(registration -> event.isBlank() || registration.getEvent().getSlug().equals(event))
            .filter(registration -> needle.isBlank()
                || registration.getFullName().toLowerCase(Locale.ROOT).contains(needle)
                || registration.getEmail().toLowerCase(Locale.ROOT).contains(needle)
                || registration.getEvent().getTitle().toLowerCase(Locale.ROOT).contains(needle))
            .map(this::toResponse)
            .toList();
        long tickets = paid.stream().mapToLong(EventRegistration::getGuestCount).sum();
        BigDecimal revenue = paid.stream().map(EventRegistration::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long checkedIn = paid.stream().filter(item -> item.getCheckedInAt() != null).mapToLong(EventRegistration::getGuestCount).sum();
        return new AdminEventRegistrationsResponse(
            new AdminEventRegistrationSummary(paid.size(), tickets, revenue, checkedIn), visible);
    }

    private AdminEventRegistrationResponse toResponse(EventRegistration registration) {
        String compactId = registration.getPublicId().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return new AdminEventRegistrationResponse(
            registration.getPublicId(), registration.getFullName(), registration.getEmail(), registration.getPhone(),
            registration.getEvent().getTitle(), registration.getEvent().getSlug(), registration.getGuestCount(),
            registration.getTotalAmount(), "PAID", registration.getConfirmedAt() == null ? registration.getCreatedAt() : registration.getConfirmedAt(),
            "CSC-" + compactId, registration.getCheckedInAt(), tickets.issue(registration.getPublicId()),
            registration.getConfirmationEmailSentAt(), registration.getConfirmationEmailError()
        );
    }

    @Transactional
    public AdminEventRegistrationResponse sendConfirmationEmail(java.util.UUID registrationId) {
        EventRegistration registration = registrations.findByPublicId(registrationId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Registration not found."));
        if (registration.getStatus() != EventRegistrationStatus.CONFIRMED)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "This registration is not confirmed.");
        confirmationEmails.sendIfNeeded(registration);
        return toResponse(registration);
    }

    @Transactional(readOnly = true)
    public TicketCheckInResponse validate(String ticketToken) {
        EventRegistration registration = findConfirmed(ticketToken);
        return response(registration, registration.getCheckedInAt() == null ? "VALID" : "ALREADY_CHECKED_IN");
    }

    @Transactional
    public TicketCheckInResponse checkIn(String ticketToken) {
        EventRegistration registration = findConfirmed(ticketToken);
        if (registration.getCheckedInAt() != null) return response(registration, "ALREADY_CHECKED_IN");
        registration.checkIn();
        registrations.save(registration);
        return response(registration, "CHECKED_IN");
    }

    @Transactional
    public TicketCheckInResponse undo(String ticketToken) {
        EventRegistration registration = findConfirmed(ticketToken);
        registration.undoCheckIn();
        registrations.save(registration);
        return response(registration, "VALID");
    }

    private EventRegistration findConfirmed(String ticketToken) {
        var id = tickets.verify(ticketToken);
        var registration = registrations.findByPublicId(id)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Ticket not found."));
        if (registration.getStatus() != EventRegistrationStatus.CONFIRMED)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "This registration is not confirmed.");
        return registration;
    }

    private TicketCheckInResponse response(EventRegistration registration, String status) {
        String compactId = registration.getPublicId().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return new TicketCheckInResponse(status, "CSC-" + compactId, registration.getFullName(), registration.getEmail(),
            registration.getGuestCount(), registration.getEvent().getTitle(), registration.getEvent().getSlug(), registration.getCheckedInAt());
    }
}
