package com.cornercircle.backend.membership.dto;

import com.cornercircle.backend.membership.model.MembershipStatus;
import java.util.UUID;
import java.time.LocalDate;

public record MembershipStatusResponse(
    UUID applicationId,
    MembershipStatus status,
    boolean paymentComplete,
    boolean recurring,
    boolean cancelAtPeriodEnd,
    LocalDate currentPeriodEnd
) {}
