package com.cornercircle.backend.adminauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAuthServiceTest {
    @Test
    void bootstrapsCredentialAndAuthenticatesConfiguredPassword() {
        var repository = mock(AdminCredentialRepository.class);
        var stored = new AtomicReference<AdminCredential>();
        when(repository.count()).thenAnswer(ignored -> stored.get() == null ? 0L : 1L);
        when(repository.findById(1L)).thenAnswer(ignored -> Optional.ofNullable(stored.get()));
        when(repository.findByUsernameIgnoreCase(any())).thenAnswer(ignored -> Optional.ofNullable(stored.get()));
        when(repository.findByEmailIgnoreCase(any())).thenAnswer(ignored -> Optional.ofNullable(stored.get()));
        when(repository.save(any())).thenAnswer(invocation -> { var value = invocation.<AdminCredential>getArgument(0); stored.set(value); return value; });
        var service = new AdminAuthService(repository, new ObjectMapper(), "admin@example.com", "a-secure-bootstrap-password", "admin@example.com", "", "", "http://localhost:3000");

        assertTrue(service.authenticate("admin@example.com", "a-secure-bootstrap-password"));
        assertFalse(service.authenticate("admin@example.com", "wrong-password"));
        verify(repository, atLeastOnce()).save(any(AdminCredential.class));
    }

    @Test
    void rejectsInvalidOrWeakReset() {
        var repository = mock(AdminCredentialRepository.class);
        when(repository.findByResetTokenHash(any())).thenReturn(Optional.empty());
        var service = new AdminAuthService(repository, new ObjectMapper(), "", "", "", "", "", "http://localhost:3000");

        assertFalse(service.resetPassword("invalid", "short"));
        assertFalse(service.resetPassword("invalid", "a-long-enough-password"));
    }
}
