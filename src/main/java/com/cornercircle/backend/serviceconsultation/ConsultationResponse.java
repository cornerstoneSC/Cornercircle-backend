package com.cornercircle.backend.serviceconsultation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConsultationResponse(UUID id, ConsultationType type, ConsultationStatus status, String name, String email,
    String phone, String organization, LocalDate preferredDate, String preferredTime, String clientNotes,
    String adminNotes, String googleCalendarEventId, Instant createdAt, Instant updatedAt) {
    static ConsultationResponse from(ServiceConsultation value) {
        return new ConsultationResponse(value.getId(),value.getType(),value.getStatus(),value.getName(),value.getEmail(),
            value.getPhone(),value.getOrganization(),value.getPreferredDate(),value.getPreferredTime(),value.getClientNotes(),
            value.getAdminNotes(),value.getGoogleCalendarEventId(),value.getCreatedAt(),value.getUpdatedAt());
    }
}
