package com.cornercircle.backend.events.repository;

import com.cornercircle.backend.events.model.Event;
import com.cornercircle.backend.events.model.EventPublicationStatus;
import com.cornercircle.backend.events.model.EventVisibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface EventRepository
        extends JpaRepository<Event, Long> {

    Optional<Event> findBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.slug = :slug")
    Optional<Event> findBySlugForRegistration(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    Optional<Event>
    findBySlugAndPublicationStatusAndVisibility(
            String slug,
            EventPublicationStatus publicationStatus,
            EventVisibility visibility
    );

    // -------------------------------------------------
    // ALL EVENTS
    // -------------------------------------------------

    List<Event> findAllByOrderByCreatedAtDesc();

    // -------------------------------------------------
    // UPCOMING
    // -------------------------------------------------

    @Query("""
        SELECT e
        FROM Event e
        WHERE e.publicationStatus = :status
          AND e.eventDate IS NOT NULL
          AND (
                e.eventDate > :today
                OR (
                    e.eventDate = :today
                    AND e.endTime >= :currentTime
                )
          )
        ORDER BY e.eventDate ASC, e.startTime ASC
    """)
    List<Event> findUpcomingEvents(
            @Param("status")
            EventPublicationStatus status,

            @Param("today")
            LocalDate today,

            @Param("currentTime")
            LocalTime currentTime
    );

    // -------------------------------------------------
    // PAST
    // -------------------------------------------------

    @Query("""
        SELECT e
        FROM Event e
        WHERE e.publicationStatus = :status
          AND e.eventDate IS NOT NULL
          AND (
                e.eventDate < :today
                OR (
                    e.eventDate = :today
                    AND e.endTime < :currentTime
                )
          )
        ORDER BY e.eventDate DESC, e.startTime DESC
    """)
    List<Event> findPastEvents(
            @Param("status")
            EventPublicationStatus status,

            @Param("today")
            LocalDate today,

            @Param("currentTime")
            LocalTime currentTime
    );

    // -------------------------------------------------
    // DRAFTS
    // -------------------------------------------------

    List<Event>
    findAllByPublicationStatusOrderByUpdatedAtDesc(
            EventPublicationStatus publicationStatus
    );

    // -------------------------------------------------
    // PUBLIC UPCOMING
    // -------------------------------------------------

    @Query("""
        SELECT e
        FROM Event e
        WHERE e.publicationStatus = :status
          AND e.visibility = :visibility
          AND e.eventDate IS NOT NULL
          AND e.endTime IS NOT NULL
          AND (
                e.eventDate > :today
                OR (
                    e.eventDate = :today
                    AND e.endTime >= :currentTime
                )
          )
        ORDER BY e.eventDate ASC, e.startTime ASC
    """)
    List<Event> findPublicUpcomingEvents(
            @Param("status")
            EventPublicationStatus status,

            @Param("visibility")
            EventVisibility visibility,

            @Param("today")
            LocalDate today,

            @Param("currentTime")
            LocalTime currentTime,

            Pageable pageable
    );
}
