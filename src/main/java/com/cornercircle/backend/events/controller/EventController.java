package com.cornercircle.backend.events.controller;

import com.cornercircle.backend.events.dto.PublicEventResponse;
import com.cornercircle.backend.events.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(
            EventService eventService
    ) {
        this.eventService = eventService;
    }

    // --------------------------------------------
    // PUBLIC EVENTS PAGE
    // --------------------------------------------

    @GetMapping
    public ResponseEntity<List<PublicEventResponse>>
    getUpcomingEvents() {

        return ResponseEntity.ok(
                eventService.getPublicUpcomingEvents()
        );
    }

    // --------------------------------------------
    // HOMEPAGE
    // --------------------------------------------

    @GetMapping("/homepage")
    public ResponseEntity<List<PublicEventResponse>>
    getHomepageEvents() {

        return ResponseEntity.ok(
                eventService.getHomepageEvents()
        );
    }

    // --------------------------------------------
    // VIEW DETAILS
    // --------------------------------------------

    @GetMapping("/{slug}")
    public ResponseEntity<PublicEventResponse>
    getEventBySlug(
            @PathVariable String slug
    ) {

        return ResponseEntity.ok(
                eventService.getPublicEventBySlug(slug)
        );
    }
}
