package com.cornercircle.backend.registration.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminEventRegistrationResponse(
    UUID registrationId,
    String fullName,
    String email,
    String phone,
    String eventTitle,
    String eventSlug,
    Integer ticketQuantity,
    BigDecimal amountPaid,
    String paymentStatus,
    LocalDateTime registrationDate,
    String confirmationNumber,
    LocalDateTime checkedInAt,
    String ticketToken
) {}
