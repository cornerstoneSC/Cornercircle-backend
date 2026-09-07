package com.cornercircle.backend.registration.dto;

import java.math.BigDecimal;

public record AdminEventRegistrationSummary(
    long paidRegistrations,
    long ticketsSold,
    BigDecimal revenue,
    long checkedInTickets
) {}
