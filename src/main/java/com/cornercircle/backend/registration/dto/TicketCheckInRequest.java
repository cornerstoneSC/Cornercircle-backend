package com.cornercircle.backend.registration.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketCheckInRequest(@NotBlank String ticketToken) {}
