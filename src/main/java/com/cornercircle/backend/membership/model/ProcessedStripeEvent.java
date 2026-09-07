package com.cornercircle.backend.membership.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_stripe_events")
public class ProcessedStripeEvent {
    @Id @Column(name = "event_id", length = 255) private String eventId;
    @Column(name = "event_type", nullable = false, length = 120) private String eventType;
    @Column(name = "processed_at", nullable = false, updatable = false) private LocalDateTime processedAt;
    protected ProcessedStripeEvent() {}
    public ProcessedStripeEvent(String eventId, String eventType) { this.eventId = eventId; this.eventType = eventType; this.processedAt = LocalDateTime.now(); }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
