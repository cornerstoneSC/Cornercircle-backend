package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.model.MembershipApplication;
import com.cornercircle.backend.membership.model.MembershipStatus;
import com.cornercircle.backend.membership.repository.MembershipApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipEmailServiceTest {
    @Test void missingEmailConfigurationDoesNotDeactivateMember() {
        var repository = mock(MembershipApplicationRepository.class);
        var member = new MembershipApplication(); member.setStatus(MembershipStatus.ACTIVE);
        var service = new MembershipEmailService(repository, new ObjectMapper(), "", "", "", "http://localhost:3000");
        service.sendIfNeeded(member, false);
        assertEquals(MembershipStatus.ACTIVE, member.getStatus());
        assertEquals("Email delivery is not configured.", member.getWelcomeEmailError());
        verify(repository).save(member);
    }
}
