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

        assertEquals(AdminAuthService.AuthenticationResult.AUTHENTICATED, service.authenticate("admin@example.com", "a-secure-bootstrap-password"));
        assertEquals(AdminAuthService.AuthenticationResult.REJECTED, service.authenticate("admin@example.com", "wrong-password"));
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

    @Test
    void authorizesGoogleLoginOnlyForAnActiveAdministratorEmail() {
        var repository = mock(AdminCredentialRepository.class);
        var admin = new AdminCredential(2L, "Google Admin", "admin@example.com", "unused");
        when(repository.count()).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase(anyString())).thenAnswer(invocation ->
                invocation.<String>getArgument(0).trim().equalsIgnoreCase("admin@example.com") ? Optional.of(admin) : Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new AdminAuthService(repository, new ObjectMapper(), "", "", "", "", "", "http://localhost:3000");

        assertEquals("admin@example.com", service.authenticateGoogle(" ADMIN@example.com ").email());
        assertNull(service.authenticateGoogle("other@example.com"));
        verify(repository).save(admin);
    }
}
