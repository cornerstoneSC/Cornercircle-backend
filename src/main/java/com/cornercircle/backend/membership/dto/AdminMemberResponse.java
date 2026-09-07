package com.cornercircle.backend.membership.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminMemberResponse(
    UUID applicationId,
    String fullName,
    String email,
    String phone,
    String city,
    LocalDate birthday,
    String paymentStatus,
    int amountCents,
    LocalDateTime paidAt,
    LocalDate membershipStartsOn,
    LocalDate membershipEndsOn,
    LocalDateTime renewalReminderSentAt,
    List<String> activities,
    List<String> goals,
    boolean membershipAgreementAccepted,
    boolean photographyNoticeAcknowledged,
    String inspiredBy,
    String comments,
    String internalNotes,
    String stripeCheckoutSessionId,
    LocalDateTime joinedAt
) {}
