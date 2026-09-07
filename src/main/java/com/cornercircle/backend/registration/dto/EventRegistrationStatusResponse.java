package com.cornercircle.backend.registration.dto;

import java.util.UUID;

public record EventRegistrationStatusResponse(UUID registrationId, String status, String email) {}
