package com.cornercircle.backend.registration.dto;

import java.util.List;

public record AdminEventRegistrationsResponse(
    AdminEventRegistrationSummary summary,
    List<AdminEventRegistrationResponse> registrations
) {}
