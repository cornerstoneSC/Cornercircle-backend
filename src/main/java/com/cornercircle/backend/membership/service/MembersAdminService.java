package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.dto.*;
import com.cornercircle.backend.membership.model.MembershipApplication;
import com.cornercircle.backend.membership.model.MembershipStatus;
import com.cornercircle.backend.membership.repository.MembershipApplicationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MembersAdminService {
    private static final int ANNUAL_MEMBERSHIP_CENTS = 19_900;
    private final MembershipApplicationRepository applications;

    public MembersAdminService(MembershipApplicationRepository applications) {
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    public AdminMembersResponse list(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        var all = applications.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        var visible = all.stream()
            .filter(application -> needle.isBlank()
                || application.getFullName().toLowerCase(Locale.ROOT).contains(needle)
                || application.getEmail().toLowerCase(Locale.ROOT).contains(needle)
                || application.getCity().toLowerCase(Locale.ROOT).contains(needle))
            .map(this::toResponse)
            .toList();
        long paid = all.stream().filter(application -> "PAID".equals(paymentStatus(application))).count();
        long pending = all.stream().filter(application -> "PENDING".equals(paymentStatus(application))).count();
        return new AdminMembersResponse(new AdminMemberSummary(all.size(), paid, pending), visible);
    }

    @Transactional(readOnly = true)
    public AdminMemberResponse get(UUID publicId) {
        return toResponse(require(publicId));
    }

    @Transactional
    public AdminMemberResponse recordReminder(UUID publicId) {
        var application = require(publicId);
        application.setRenewalReminderSentAt(LocalDateTime.now());
        return toResponse(application);
    }

    @Transactional
    public AdminMemberResponse updateNotes(UUID publicId, MemberNotesRequest request) {
        var application = require(publicId);
        String notes = request.internalNotes();
        if (notes != null && notes.length() > 5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Internal notes must be 5,000 characters or fewer.");
        application.setInternalNotes(notes == null || notes.isBlank() ? null : notes.trim());
        return toResponse(application);
    }

    private MembershipApplication require(UUID publicId) {
        return applications.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found."));
    }

    private AdminMemberResponse toResponse(MembershipApplication application) {
        return new AdminMemberResponse(
            application.getPublicId(), application.getFullName(), application.getEmail(), application.getPhone(),
            application.getCity(), application.getBirthday(), paymentStatus(application), ANNUAL_MEMBERSHIP_CENTS,
            application.getPaidAt(), application.getMembershipStartsOn(), application.getMembershipEndsOn(),
            application.getRenewalReminderSentAt(), List.copyOf(application.getActivities()), List.copyOf(application.getGoals()),
            application.isMembershipAgreementAccepted(), application.isPhotographyNoticeAcknowledged(),
            application.getInspiredBy(), application.getComments(), application.getInternalNotes(),
            application.getStripeCheckoutSessionId(), application.getCreatedAt()
        );
    }

    private static String paymentStatus(MembershipApplication application) {
        if (application.getStatus() == MembershipStatus.REFUNDED) return "REFUNDED";
        if (application.getStatus() == MembershipStatus.PAYMENT_FAILED || application.getStatus() == MembershipStatus.PAST_DUE) return "FAILED";
        if (application.getStatus() == MembershipStatus.EXPIRED
            || application.getMembershipEndsOn() != null && application.getMembershipEndsOn().isBefore(LocalDate.now())) return "EXPIRED";
        if (application.getStatus() == MembershipStatus.ACTIVE) return "PAID";
        return "PENDING";
    }
}
