package com.cornercircle.backend.serviceconsultation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ConsultationCreateRequest(
    @NotNull ConsultationType type,
    @NotBlank @Size(max=120) String name,
    @NotBlank @Email @Size(max=180) String email,
    @Size(max=40) String phone,
    @Size(max=180) String organization,
    LocalDate preferredDate,
    @Size(max=40) String preferredTime,
    @Size(max=3000) String notes
) {}
