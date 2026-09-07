package com.cornercircle.backend.events.service;

import com.cornercircle.backend.events.dto.EventExpectationRequest;
import com.cornercircle.backend.events.dto.EventExpectationResponse;
import com.cornercircle.backend.events.dto.EventRequest;
import com.cornercircle.backend.events.dto.EventResponse;
import com.cornercircle.backend.events.dto.PublicEventResponse;
import com.cornercircle.backend.events.model.Event;
import com.cornercircle.backend.events.model.EventExpectation;
import com.cornercircle.backend.events.model.EventPublicationStatus;
import com.cornercircle.backend.events.model.EventVisibility;
import com.cornercircle.backend.events.repository.EventRepository;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EventService {

    private static final ZoneId SITE_ZONE =
            ZoneId.of("America/Los_Angeles");

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;

    public EventService(EventRepository eventRepository, EventRegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    // -------------------------------------------------
    // HELPER
    // -------------------------------------------------

    private Event findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Event not found"
                        )
                );
    }

    // -------------------------------------------------
    // CREATE
    // -------------------------------------------------

    public EventResponse createEvent(EventRequest request) {

        Event event = new Event();

        event.setTitle(request.getTitle().trim());
        event.setSlug(generateUniqueSlug(request.getTitle()));

        applyRequest(event, request);

        Event savedEvent = eventRepository.save(event);

        return toResponse(savedEvent);
    }

    // -------------------------------------------------
    // GET ALL
    // -------------------------------------------------

    public List<EventResponse> getAllEvents() {

        return eventRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------
    // GET ONE
    // -------------------------------------------------

    public EventResponse getEventById(Long id) {
        return toResponse(findEvent(id));
    }

    // -------------------------------------------------
    // UPDATE
    // -------------------------------------------------

    @Transactional
    public EventResponse updateEvent(
            Long id,
            EventRequest request
    ) {
        Event event = findEvent(id);

        event.setTitle(request.getTitle().trim());

        /*
         * Keep the existing slug.
         *
         * If:
         * Summer Mix & Mingle
         *
         * becomes:
         * Summer Mix & Mingle 2026
         *
         * the public URL does not suddenly break.
         */

        applyRequest(event, request);

        Event savedEvent = eventRepository.save(event);

        return toResponse(savedEvent);
    }

    // -------------------------------------------------
    // DELETE
    // -------------------------------------------------

    @Transactional
    public void deleteEvent(Long id) {
        Event event = findEvent(id);
        if (registrationRepository.countByEventId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Events with registrations cannot be deleted. Hide or unpublish this event instead.");
        }
        eventRepository.delete(event);
    }

    // -------------------------------------------------
    // UPCOMING EVENTS
    // -------------------------------------------------

    public List<EventResponse> getUpcomingEvents() {
        ZonedDateTime now = ZonedDateTime.now(SITE_ZONE);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        return eventRepository
                .findUpcomingEvents(
                        EventPublicationStatus.PUBLISHED,
                        today,
                        currentTime
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------
    // PAST EVENTS
    // -------------------------------------------------

    public List<EventResponse> getPastEvents() {
        ZonedDateTime now = ZonedDateTime.now(SITE_ZONE);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        return eventRepository
                .findPastEvents(
                        EventPublicationStatus.PUBLISHED,
                        today,
                        currentTime
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------
    // DRAFT EVENTS
    // -------------------------------------------------

    public List<EventResponse> getDraftEvents() {
        return eventRepository
                .findAllByPublicationStatusOrderByUpdatedAtDesc(
                        EventPublicationStatus.DRAFT
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------
    // PUBLIC UPCOMING EVENTS
    // -------------------------------------------------

    @Transactional(readOnly = true)
    public List<PublicEventResponse> getPublicUpcomingEvents() {
        ZonedDateTime now =
                ZonedDateTime.now(SITE_ZONE);

        return eventRepository
                .findPublicUpcomingEvents(
                        EventPublicationStatus.PUBLISHED,
                        EventVisibility.PUBLIC,
                        now.toLocalDate(),
                        now.toLocalTime(),
                        Pageable.unpaged()
                )
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    // -------------------------------------------------
    // HOMEPAGE EVENTS
    // -------------------------------------------------

    @Transactional(readOnly = true)
    public List<PublicEventResponse> getHomepageEvents() {
        ZonedDateTime now =
                ZonedDateTime.now(SITE_ZONE);

        return eventRepository
                .findPublicUpcomingEvents(
                        EventPublicationStatus.PUBLISHED,
                        EventVisibility.PUBLIC,
                        now.toLocalDate(),
                        now.toLocalTime(),
                        PageRequest.of(0, 3)
                )
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    // -------------------------------------------------
    // PUBLIC EVENT BY SLUG
    // -------------------------------------------------

    @Transactional(readOnly = true)
    public PublicEventResponse getPublicEventBySlug(
            String slug
    ) {
        Event event = eventRepository
                .findBySlugAndPublicationStatusAndVisibility(
                        slug,
                        EventPublicationStatus.PUBLISHED,
                        EventVisibility.PUBLIC
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Event not found"
                        )
                );

        ZonedDateTime now =
                ZonedDateTime.now(SITE_ZONE);

        if (hasEventEnded(
                event,
                now.toLocalDate(),
                now.toLocalTime()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Event not found"
            );
        }

        return toPublicResponse(event);
    }

    // -------------------------------------------------
    // CHANGE VISIBILITY
    // -------------------------------------------------

    @Transactional
    public EventResponse changeVisibility(
            Long id,
            EventVisibility visibility
    ) {
        Event event = findEvent(id);

        if (
                event.getPublicationStatus()
                        == EventPublicationStatus.DRAFT
                &&
                visibility == EventVisibility.PUBLIC
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A draft event cannot be public"
            );
        }

        event.setVisibility(visibility);

        return toResponse(
                eventRepository.save(event)
        );
    }

    // -------------------------------------------------
    // PUBLISH EVENT
    // -------------------------------------------------

    @Transactional
    public EventResponse publishEvent(Long id) {
        Event event = findEvent(id);

        validateEventForPublication(event);

        event.setPublicationStatus(
                EventPublicationStatus.PUBLISHED
        );

        event.setVisibility(
                EventVisibility.PUBLIC
        );

        return toResponse(
                eventRepository.save(event)
        );
    }

    // -------------------------------------------------
    // UNPUBLISH EVENT
    // -------------------------------------------------

    @Transactional
    public EventResponse unpublishEvent(Long id) {
        Event event = findEvent(id);

        event.setPublicationStatus(
                EventPublicationStatus.DRAFT
        );

        event.setVisibility(
                EventVisibility.HIDDEN
        );

        return toResponse(
                eventRepository.save(event)
        );
    }

    // -------------------------------------------------
    // ENTITY → REQUEST (for validation)
    // -------------------------------------------------

    private EventRequest toRequest(Event event) {
        EventRequest request = new EventRequest();
        request.setTitle(event.getTitle());
        request.setCategory(event.getCategory());
        request.setShortDescription(event.getShortDescription());
        request.setDescription(event.getDescription());
        request.setCoverImageUrl(event.getCoverImageUrl());
        request.setEventDate(event.getEventDate());
        request.setStartTime(event.getStartTime());
        request.setEndTime(event.getEndTime());
        request.setTimeZone(event.getTimeZone());
        request.setVenueName(event.getVenueName());
        request.setAddress(event.getAddress());
        request.setCity(event.getCity());
        request.setState(event.getState());
        request.setZipCode(event.getZipCode());
        request.setAdditionalInformation(event.getAdditionalInformation());
        request.setPricePerPerson(event.getPricePerPerson());
        request.setCapacity(event.getCapacity());
        request.setRegistrationDeadline(event.getRegistrationDeadline());
        return request;
    }

    // -------------------------------------------------
    // MAPPING REQUEST → ENTITY
    // -------------------------------------------------

    private void applyRequest(
            Event event,
            EventRequest request
    ) {

        EventPublicationStatus status =
                request.getPublicationStatus() == null
                        ? EventPublicationStatus.DRAFT
                        : request.getPublicationStatus();

        EventVisibility visibility =
                request.getVisibility() == null
                        ? EventVisibility.HIDDEN
                        : request.getVisibility();

        // Drafts can never be public.
        if (status == EventPublicationStatus.DRAFT) {
            visibility = EventVisibility.HIDDEN;
        }

        if (status == EventPublicationStatus.PUBLISHED) {
            validateForPublication(request);
        }

        event.setCategory(request.getCategory() == null || request.getCategory().isBlank() ? "Social gathering" : request.getCategory().trim());
        event.setShortDescription(request.getShortDescription());
        event.setDescription(request.getDescription());
        event.setCoverImageUrl(request.getCoverImageUrl());

        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());

        event.setTimeZone(
                request.getTimeZone() == null ||
                request.getTimeZone().isBlank()
                        ? "America/Los_Angeles"
                        : request.getTimeZone()
        );

        event.setVenueName(request.getVenueName());
        event.setAddress(request.getAddress());
        event.setCity(request.getCity());
        event.setState(request.getState());
        event.setZipCode(request.getZipCode());

        event.setAdditionalInformation(
                request.getAdditionalInformation()
        );

        event.setPricePerPerson(
                request.getPricePerPerson()
        );

        event.setCapacity(request.getCapacity());

        event.setRegistrationDeadline(
                request.getRegistrationDeadline()
        );

        event.setPublicationStatus(status);
        event.setVisibility(visibility);

        event.getExpectations().clear();

        if (request.getExpectations() != null) {

            for (EventExpectationRequest item
                    : request.getExpectations()) {

                event.getExpectations().add(
                        new EventExpectation(
                                item.getTitle(),
                                item.getDescription()
                        )
                );
            }
        }
    }

    // -------------------------------------------------
    // PUBLISH VALIDATION
    // -------------------------------------------------

    private void validateEventForPublication(Event event) {
        requireText(
                event.getShortDescription(),
                "Short description is required before publishing"
        );

        requireText(
                event.getDescription(),
                "Description is required before publishing"
        );

        requireText(
                event.getCoverImageUrl(),
                "Cover image is required before publishing"
        );

        if (event.getEventDate() == null) {
            badRequest("Event date is required before publishing");
        }

        if (event.getStartTime() == null) {
            badRequest(
                    "Start time is required before publishing"
            );
        }

        if (event.getEndTime() == null) {
            badRequest(
                    "End time is required before publishing"
            );
        }

        if (
                event.getStartTime() != null &&
                event.getEndTime() != null &&
                !event.getEndTime()
                        .isAfter(event.getStartTime())
        ) {
            badRequest(
                    "End time must be after start time"
            );
        }

        requireText(
                event.getVenueName(),
                "Venue name is required before publishing"
        );

        requireText(
                event.getAddress(),
                "Address is required before publishing"
        );

        requireText(
                event.getCity(),
                "City is required before publishing"
        );

        requireText(
                event.getState(),
                "State is required before publishing"
        );

        if (event.getPricePerPerson() == null) {
            badRequest(
                    "Price is required before publishing"
            );
        }

        if (event.getPricePerPerson().signum() < 0) {
            badRequest(
                    "Price cannot be negative"
            );
        }

        if (
                event.getCapacity() == null ||
                event.getCapacity() <= 0
        ) {
            badRequest(
                    "Capacity must be greater than zero"
            );
        }

        if (
                event.getRegistrationDeadline() != null &&
                event.getEventDate() != null &&
                event.getStartTime() != null &&
                event.getRegistrationDeadline()
                        .isAfter(
                                event.getEventDate()
                                        .atTime(
                                                event.getStartTime()
                                        )
                        )
        ) {
            badRequest(
                    "Registration deadline must be before the event starts"
            );
        }
        validateSchedule(event.getEventDate(), event.getStartTime(), event.getEndTime(), event.getRegistrationDeadline(), event.getTimeZone());
    }

    private void validateForPublication(
            EventRequest request
    ) {

        requireText(
                request.getShortDescription(),
                "Short description is required before publishing"
        );

        requireText(
                request.getDescription(),
                "Description is required before publishing"
        );

        requireText(
                request.getCoverImageUrl(),
                "Cover image is required before publishing"
        );

        if (request.getEventDate() == null) {
            badRequest("Event date is required before publishing");
        }

        if (request.getStartTime() == null) {
            badRequest("Start time is required before publishing");
        }

        if (request.getEndTime() == null) {
            badRequest("End time is required before publishing");
        }

        if (
                request.getStartTime() != null &&
                request.getEndTime() != null &&
                !request.getEndTime()
                        .isAfter(request.getStartTime())
        ) {
            badRequest(
                    "End time must be after start time"
            );
        }

        requireText(
                request.getVenueName(),
                "Venue name is required before publishing"
        );

        requireText(
                request.getAddress(),
                "Address is required before publishing"
        );

        requireText(
                request.getCity(),
                "City is required before publishing"
        );

        requireText(
                request.getState(),
                "State is required before publishing"
        );

        if (request.getPricePerPerson() == null) {
            badRequest(
                    "Price is required before publishing"
            );
        }

        if (
                request.getPricePerPerson() != null &&
                request.getPricePerPerson().signum() < 0
        ) {
            badRequest(
                    "Price cannot be negative"
            );
        }

        if (
                request.getCapacity() == null ||
                request.getCapacity() <= 0
        ) {
            badRequest(
                    "Capacity must be greater than zero"
            );
        }

        if (
                request.getRegistrationDeadline() != null &&
                request.getEventDate() != null &&
                request.getStartTime() != null &&
                request.getRegistrationDeadline()
                        .isAfter(
                                request.getEventDate()
                                        .atTime(
                                                request.getStartTime()
                                        )
                        )
        ) {
            badRequest(
                    "Registration deadline must be before the event starts"
            );
        }
        validateSchedule(request.getEventDate(), request.getStartTime(), request.getEndTime(), request.getRegistrationDeadline(), request.getTimeZone());
    }

    private void validateSchedule(LocalDate date, LocalTime start, LocalTime end, java.time.LocalDateTime deadline, String timeZone) {
        ZoneId zone;
        try { zone = ZoneId.of(timeZone == null || timeZone.isBlank() ? SITE_ZONE.getId() : timeZone); }
        catch (Exception exception) { badRequest("Select a valid event time zone"); return; }
        ZonedDateTime now = ZonedDateTime.now(zone);
        if (date != null && end != null && !date.atTime(end).atZone(zone).isAfter(now))
            badRequest("Event must end in the future");
        if (deadline != null && !deadline.atZone(zone).isAfter(now))
            badRequest("Registration deadline must be in the future");
    }

    private void requireText(
            String value,
            String message
    ) {

        if (value == null || value.isBlank()) {
            badRequest(message);
        }
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    // -------------------------------------------------
    // EVENT END CHECK
    // -------------------------------------------------

    private boolean hasEventEnded(
            Event event,
            LocalDate today,
            LocalTime currentTime
    ) {
        if (event.getEventDate() == null) {
            return true;
        }

        if (event.getEventDate().isBefore(today)) {
            return true;
        }

        if (
                event.getEventDate().isEqual(today)
                && event.getEndTime() != null
                && event.getEndTime().isBefore(currentTime)
        ) {
            return true;
        }

        return false;
    }

    // -------------------------------------------------
    // SLUG
    // -------------------------------------------------

    private String generateUniqueSlug(String title) {

        String normalized =
                Normalizer.normalize(
                        title,
                        Normalizer.Form.NFD
                );

        String slug = normalized
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isBlank()) {
            slug = "event";
        }

        String candidate = slug;
        int suffix = 2;

        while (eventRepository.existsBySlug(candidate)) {
            candidate = slug + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    // -------------------------------------------------
    // ENTITY → RESPONSE
    // -------------------------------------------------

    private EventResponse toResponse(Event event) {

        EventResponse response = new EventResponse();

        response.setId(event.getId());
        response.setTitle(event.getTitle());
        response.setSlug(event.getSlug());
        response.setCategory(event.getCategory());

        response.setShortDescription(
                event.getShortDescription()
        );

        response.setDescription(
                event.getDescription()
        );

        response.setCoverImageUrl(
                event.getCoverImageUrl()
        );

        response.setEventDate(event.getEventDate());
        response.setStartTime(event.getStartTime());
        response.setEndTime(event.getEndTime());
        response.setTimeZone(event.getTimeZone());

        response.setVenueName(event.getVenueName());
        response.setAddress(event.getAddress());
        response.setCity(event.getCity());
        response.setState(event.getState());
        response.setZipCode(event.getZipCode());

        response.setAdditionalInformation(
                event.getAdditionalInformation()
        );

        response.setPricePerPerson(
                event.getPricePerPerson()
        );

        response.setCapacity(event.getCapacity());

        if (event.getCapacity() != null) {
            long reservedSeats = registrationRepository.activeReservedSeats(
                    event.getId(),
                    java.time.LocalDateTime.now().minusMinutes(20)
            );
            response.setRemainingCapacity(
                    Math.max(0, event.getCapacity() - Math.toIntExact(reservedSeats))
            );
        }

        response.setRegistrationDeadline(
                event.getRegistrationDeadline()
        );

        response.setPublicationStatus(
                event.getPublicationStatus()
        );

        response.setVisibility(
                event.getVisibility()
        );

        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());

        List<EventExpectationRequest> expectations =
                event.getExpectations()
                        .stream()
                        .map(expectation -> {

                            EventExpectationRequest dto =
                                    new EventExpectationRequest();

                            dto.setTitle(
                                    expectation.getTitle()
                            );

                            dto.setDescription(
                                    expectation.getDescription()
                            );

                            return dto;
                        })
                        .toList();

        response.setExpectations(expectations);

        return response;
    }

    // -------------------------------------------------
    // ENTITY → PUBLIC RESPONSE
    // -------------------------------------------------

    private PublicEventResponse toPublicResponse(Event event) {
        List<EventExpectationResponse> expectations =
                event.getExpectations()
                        .stream()
                        .map(expectation ->
                                new EventExpectationResponse(
                                        expectation.getTitle(),
                                        expectation.getDescription()
                                )
                        )
                        .toList();

        int remaining = Math.max(0, event.getCapacity() - Math.toIntExact(registrationRepository.activeReservedSeats(event.getId(), java.time.LocalDateTime.now().minusMinutes(20))));
        ZoneId zone;
        try { zone = ZoneId.of(event.getTimeZone()); }
        catch (Exception ignored) { zone = SITE_ZONE; }
        ZonedDateTime now = ZonedDateTime.now(zone);
        boolean deadlineOpen = event.getRegistrationDeadline() == null || !event.getRegistrationDeadline().atZone(zone).isBefore(now);
        boolean registrationOpen = remaining > 0 && deadlineOpen && event.getEventDate().atTime(event.getEndTime()).atZone(zone).isAfter(now);

        return new PublicEventResponse(
                event.getId(),
                event.getTitle(),
                event.getSlug(),
                event.getCategory() == null ? "Social gathering" : event.getCategory(),
                event.getShortDescription(),
                event.getDescription(),
                event.getCoverImageUrl(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getTimeZone(),
                event.getVenueName(),
                event.getAddress(),
                event.getCity(),
                event.getState(),
                event.getZipCode(),
                expectations,
                event.getAdditionalInformation(),
                event.getPricePerPerson(),
                event.getCapacity(),
                event.getRegistrationDeadline(),
                registrationOpen
        );
    }
}
