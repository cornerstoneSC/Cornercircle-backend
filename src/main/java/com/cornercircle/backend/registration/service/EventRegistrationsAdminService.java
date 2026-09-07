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

    public EventRegistrationsAdminService(EventRegistrationRepository registrations) {
        this.registrations = registrations;
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
        return new AdminEventRegistrationsResponse(
            new AdminEventRegistrationSummary(paid.size(), tickets, revenue), visible);
    }

    private AdminEventRegistrationResponse toResponse(EventRegistration registration) {
        String compactId = registration.getPublicId().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return new AdminEventRegistrationResponse(
            registration.getPublicId(), registration.getFullName(), registration.getEmail(), registration.getPhone(),
            registration.getEvent().getTitle(), registration.getEvent().getSlug(), registration.getGuestCount(),
            registration.getTotalAmount(), "PAID", registration.getConfirmedAt() == null ? registration.getCreatedAt() : registration.getConfirmedAt(),
            "CSC-" + compactId
        );
    }
}
