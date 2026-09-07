package com.cornercircle.backend.registration.dto;
import jakarta.validation.constraints.*;

public record EventRegistrationRequest(
        @NotBlank @Size(max=120) String fullName,
        @NotBlank @Email @Size(max=200) String email,
        @Size(max=40) String phone,
        @NotNull @Min(1) @Max(10) Integer guestCount,
        @AssertTrue(message="You must confirm that every attendee is at least 21 years old.") Boolean ageConfirmed,
        @AssertTrue(message="You must accept the Event Terms and Cancellation Policy.") Boolean termsAccepted
) {}
