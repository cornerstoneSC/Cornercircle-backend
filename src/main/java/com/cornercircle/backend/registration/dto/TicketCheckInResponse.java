package com.cornercircle.backend.registration.dto;

import java.time.LocalDateTime;

public record TicketCheckInResponse(
    String status,
    String confirmationNumber,
    String fullName,
    String email,
    int guestCount,
    String eventTitle,
    String eventSlug,
    LocalDateTime checkedInAt
) {}
