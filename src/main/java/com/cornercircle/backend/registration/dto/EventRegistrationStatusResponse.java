package com.cornercircle.backend.registration.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EventRegistrationStatusResponse(
    UUID registrationId, String status, String email, String fullName, int guestCount,
    String confirmationNumber, String ticketToken, String eventTitle, LocalDate eventDate,
    LocalTime startTime, LocalTime endTime, String timeZone, String venueName,
    String address, String city, String state, String zipCode
) {}
