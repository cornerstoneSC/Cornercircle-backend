package com.cornercircle.backend.registration.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

@Service
public class TicketTokenService {
    private final byte[] secret;

    public TicketTokenService(@Value("${TICKET_SIGNING_SECRET:${MEMBERSHIP_ADMIN_TOKEN:}}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(UUID registrationId) {
        requireConfigured();
        String id = registrationId.toString();
        return id + "." + signature(id);
    }

    public UUID verify(String token) {
        requireConfigured();
        if (token == null) throw invalid();
        int separator = token.lastIndexOf('.');
        if (separator <= 0) throw invalid();
        String id = token.substring(0, separator);
        byte[] expected = signature(id).getBytes(StandardCharsets.UTF_8);
        byte[] supplied = token.substring(separator + 1).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, supplied)) throw invalid();
        try { return UUID.fromString(id); }
        catch (IllegalArgumentException ignored) { throw invalid(); }
    }

    private String signature(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign event ticket.", exception);
        }
    }

    private void requireConfigured() {
        if (secret.length < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Ticket signing is not configured.");
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ticket.");
    }
}
