package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.dto.MemberNotesRequest;
import com.cornercircle.backend.membership.model.*;
import com.cornercircle.backend.membership.repository.MembershipApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MembersAdminServiceTest {
    private final MembershipApplicationRepository applications = mock(MembershipApplicationRepository.class);
    private final MembersAdminService service = new MembersAdminService(applications);

    @Test void returnsSummaryAndFiltersByNameEmailOrCity() {
        var paid = member("Maya Johnson", "maya@example.com", "Austin", MembershipStatus.ACTIVE);
        paid.setMembershipEndsOn(LocalDate.now().plusMonths(4));
        var pending = member("Elaine Carter", "elaine@example.com", "Nashville", MembershipStatus.PENDING_PAYMENT);
        when(applications.findAll(any(Sort.class))).thenReturn(List.of(paid, pending));

        var response = service.list("maya");

        assertEquals(2, response.summary().totalMembers());
        assertEquals(1, response.summary().paid());
        assertEquals(1, response.summary().paymentPending());
        assertEquals(List.of("Maya Johnson"), response.members().stream().map(member -> member.fullName()).toList());
    }

    @Test void derivesExpiredStatusFromMembershipEndDate() {
        var member = member("Past Member", "past@example.com", "Dallas", MembershipStatus.ACTIVE);
        member.setMembershipEndsOn(LocalDate.now().minusDays(1));
        when(applications.findByPublicId(member.getPublicId())).thenReturn(Optional.of(member));
        assertEquals("EXPIRED", service.get(member.getPublicId()).paymentStatus());
    }

    @Test void recordsReminderAndUpdatesInternalNotes() {
        var member = member("Maya Johnson", "maya@example.com", "Austin", MembershipStatus.ACTIVE);
        when(applications.findByPublicId(member.getPublicId())).thenReturn(Optional.of(member));

        assertNotNull(service.recordReminder(member.getPublicId()).renewalReminderSentAt());
        assertEquals("Prefers email.", service.updateNotes(member.getPublicId(), new MemberNotesRequest("  Prefers email.  ")).internalNotes());
    }

    private static MembershipApplication member(String name, String email, String city, MembershipStatus status) {
        var member = new MembershipApplication();
        member.setPublicId(UUID.randomUUID()); member.setFullName(name); member.setEmail(email); member.setCity(city);
        member.setStatus(status); member.setInspiredBy("Connection"); member.setActivities(List.of("Coffee")); member.setGoals(List.of("Friendship"));
        return member;
    }
}
