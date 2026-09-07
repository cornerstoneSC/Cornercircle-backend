package com.cornercircle.backend.registration.controller;

import com.cornercircle.backend.registration.dto.EventRegistrationRequest;
import com.cornercircle.backend.registration.dto.EventRegistrationResponse;
import com.cornercircle.backend.registration.dto.EventRegistrationStatusResponse;
import com.cornercircle.backend.registration.service.EventRegistrationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/{slug}/registrations")
public class EventRegistrationController {
    private final EventRegistrationService service;
    public EventRegistrationController(EventRegistrationService service) { this.service = service; }

    @PostMapping("/payment-intent")
    @ResponseStatus(HttpStatus.CREATED)
    public EventRegistrationResponse prepare(@PathVariable String slug, @Valid @RequestBody EventRegistrationRequest request) {
        return service.prepare(slug, request);
    }

    @GetMapping("/{registrationId}")
    public EventRegistrationStatusResponse status(@PathVariable String slug, @PathVariable UUID registrationId) {
        return service.status(slug, registrationId);
    }

    @GetMapping(value = "/{registrationId}/ticket.png", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] ticket(@PathVariable String slug, @PathVariable UUID registrationId) {
        return service.qrCode(slug, registrationId);
    }

    @GetMapping(value = "/{registrationId}/calendar.ics", produces = "text/calendar")
    public ResponseEntity<String> calendar(@PathVariable String slug, @PathVariable UUID registrationId) {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=cornerstone-event.ics")
            .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
            .body(service.calendar(slug, registrationId));
    }
}
