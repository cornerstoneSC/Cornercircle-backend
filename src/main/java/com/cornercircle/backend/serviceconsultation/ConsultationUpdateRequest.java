package com.cornercircle.backend.serviceconsultation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConsultationUpdateRequest(
    @NotNull ConsultationStatus status,
    @Size(max=3000) String adminNotes,
    @Size(max=220) String googleCalendarEventId
) {}
