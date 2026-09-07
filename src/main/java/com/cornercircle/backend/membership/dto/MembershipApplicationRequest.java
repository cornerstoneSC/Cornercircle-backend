package com.cornercircle.backend.membership.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record MembershipApplicationRequest(
    @NotBlank @Size(max = 160) String fullName,
    @NotBlank @Email @Size(max = 254) String email,
    @Size(max = 40) String phone,
    @NotBlank @Size(max = 120) String city,
    @Past LocalDate birthday,
    @NotBlank @Size(max = 3000) String inspiredBy,
    @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 200) String> activities,
    @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 200) String> goals,
    @AssertTrue(message = "The membership agreement must be accepted") boolean membershipAgreementAccepted,
    @AssertTrue(message = "The photography notice must be acknowledged") boolean photographyNoticeAcknowledged,
    @Size(max = 3000) String comments
) {}
