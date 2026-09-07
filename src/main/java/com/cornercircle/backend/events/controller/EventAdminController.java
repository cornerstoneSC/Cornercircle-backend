package com.cornercircle.backend.events.controller;

import com.cornercircle.backend.events.dto.EventImageUploadResponse;
import com.cornercircle.backend.events.dto.EventRequest;
import com.cornercircle.backend.events.dto.EventResponse;
import com.cornercircle.backend.events.dto.EventVisibilityRequest;
import com.cornercircle.backend.events.service.EventImageService;
import com.cornercircle.backend.events.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/events")
public class EventAdminController {

    private final EventService eventService;
    private final EventImageService eventImageService;

    public EventAdminController(
            EventService eventService,
            EventImageService eventImageService
    ) {
        this.eventService = eventService;
        this.eventImageService = eventImageService;
    }

    // POST /api/v1/admin/events

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request
    ) {

        EventResponse event =
                eventService.createEvent(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(event);
    }

    // POST /api/v1/admin/events/image

    @PostMapping(
            value = "/image",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<EventImageUploadResponse>
    uploadEventImage(
            @RequestParam("file")
            MultipartFile file
    ) {

        return ResponseEntity.ok(
                eventImageService.upload(file)
        );
    }

    // GET /api/v1/admin/events

    @GetMapping
    public ResponseEntity<List<EventResponse>>
    getAllEvents() {

        return ResponseEntity.ok(
                eventService.getAllEvents()
        );
    }

    // GET /api/v1/admin/events/1

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                eventService.getEventById(id)
        );
    }

    // -------------------------------------------------
    // TABS
    // -------------------------------------------------

    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {
        return ResponseEntity.ok(
                eventService.getUpcomingEvents()
        );
    }

    @GetMapping("/past")
    public ResponseEntity<List<EventResponse>> getPastEvents() {
        return ResponseEntity.ok(
                eventService.getPastEvents()
        );
    }

    @GetMapping("/drafts")
    public ResponseEntity<List<EventResponse>> getDraftEvents() {
        return ResponseEntity.ok(
                eventService.getDraftEvents()
        );
    }

    // -------------------------------------------------
    // UPDATE
    // -------------------------------------------------

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request
    ) {

        return ResponseEntity.ok(
                eventService.updateEvent(id, request)
        );
    }

    // -------------------------------------------------
    // VISIBILITY
    // -------------------------------------------------

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<EventResponse> changeVisibility(
            @PathVariable Long id,
            @Valid
            @RequestBody
            EventVisibilityRequest request
    ) {

        return ResponseEntity.ok(
                eventService.changeVisibility(
                        id,
                        request.getVisibility()
                )
        );
    }

    // -------------------------------------------------
    // PUBLISH
    // -------------------------------------------------

    @PatchMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publishEvent(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                eventService.publishEvent(id)
        );
    }

    // -------------------------------------------------
    // UNPUBLISH
    // -------------------------------------------------

    @PatchMapping("/{id}/unpublish")
    public ResponseEntity<EventResponse> unpublishEvent(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                eventService.unpublishEvent(id)
        );
    }

    // -------------------------------------------------
    // DELETE
    // -------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id
    ) {

        eventService.deleteEvent(id);

        return ResponseEntity.noContent().build();
    }
}