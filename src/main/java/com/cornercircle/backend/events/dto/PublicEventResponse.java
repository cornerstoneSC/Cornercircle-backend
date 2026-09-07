package com.cornercircle.backend.events.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record PublicEventResponse(
        Long id,
        String title,
        String slug,
        String category,

        String shortDescription,
        String description,
        String coverImageUrl,

        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String timeZone,

        String venueName,
        String address,
        String city,
        String state,
        String zipCode,

        List<EventExpectationResponse> expectations,

        String additionalInformation,

        BigDecimal pricePerPerson,
        Integer capacity,
        LocalDateTime registrationDeadline,
        boolean registrationOpen
) {
}
