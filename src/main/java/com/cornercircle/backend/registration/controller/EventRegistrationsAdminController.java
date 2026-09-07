package com.cornercircle.backend.registration.controller;

import com.cornercircle.backend.registration.dto.*;
import jakarta.validation.Valid;
import com.cornercircle.backend.registration.service.EventRegistrationsAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/admin/event-registrations")
public class EventRegistrationsAdminController {
    private final EventRegistrationsAdminService service;
    private final String adminToken;

    public EventRegistrationsAdminController(EventRegistrationsAdminService service,
        @Value("${MEMBERSHIP_ADMIN_TOKEN:}") String adminToken) {
        this.service = service;
        this.adminToken = adminToken;
    }

    @GetMapping
    public AdminEventRegistrationsResponse list(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(defaultValue = "") String query,
        @RequestParam(defaultValue = "") String event) {
        authorize(authorization);
        return service.list(query, event);
    }

    @PostMapping("/check-in/validate")
    public TicketCheckInResponse validate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @Valid @RequestBody TicketCheckInRequest request) {
        authorize(authorization);
        return service.validate(request.ticketToken());
    }

    @PostMapping("/check-in")
    public TicketCheckInResponse checkIn(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @Valid @RequestBody TicketCheckInRequest request) {
        authorize(authorization);
        return service.checkIn(request.ticketToken());
    }

    @DeleteMapping("/check-in")
    public TicketCheckInResponse undo(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @Valid @RequestBody TicketCheckInRequest request) {
        authorize(authorization);
        return service.undo(request.ticketToken());
    }

    private void authorize(String authorization) {
        if (adminToken.length() < 32)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Event registrations administration is not configured.");
        byte[] expected = ("Bearer " + adminToken).getBytes(StandardCharsets.UTF_8);
        byte[] supplied = authorization == null ? new byte[0] : authorization.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, supplied))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin token.");
    }
}
