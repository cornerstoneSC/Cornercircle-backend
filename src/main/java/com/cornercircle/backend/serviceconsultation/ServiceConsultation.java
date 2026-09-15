package com.cornercircle.backend.serviceconsultation;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "service_consultations", indexes = {
    @Index(name = "idx_service_consultation_status", columnList = "status"),
    @Index(name = "idx_service_consultation_email", columnList = "email")
})
public class ServiceConsultation {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private ConsultationType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ConsultationStatus status;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 180) private String email;
    @Column(length = 40) private String phone;
    @Column(length = 180) private String organization;
    private LocalDate preferredDate;
    @Column(length = 40) private String preferredTime;
    @Column(length = 3000) private String clientNotes;
    @Column(length = 3000) private String adminNotes;
    @Column(length = 220) private String googleCalendarEventId;
    private Instant createdAt;
    private Instant updatedAt;

    protected ServiceConsultation() {}
    public ServiceConsultation(ConsultationType type, String name, String email) {
        this.id = UUID.randomUUID(); this.type = type; this.status = ConsultationStatus.REQUESTED;
        this.name = name; this.email = email; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public UUID getId(){return id;} public ConsultationType getType(){return type;} public ConsultationStatus getStatus(){return status;}
    public String getName(){return name;} public String getEmail(){return email;} public String getPhone(){return phone;}
    public String getOrganization(){return organization;} public LocalDate getPreferredDate(){return preferredDate;}
    public String getPreferredTime(){return preferredTime;} public String getClientNotes(){return clientNotes;}
    public String getAdminNotes(){return adminNotes;} public String getGoogleCalendarEventId(){return googleCalendarEventId;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void setPhone(String value){phone=value;} public void setOrganization(String value){organization=value;}
    public void setPreferredDate(LocalDate value){preferredDate=value;} public void setPreferredTime(String value){preferredTime=value;}
    public void setClientNotes(String value){clientNotes=value;} public void setAdminNotes(String value){adminNotes=value;}
    public void setGoogleCalendarEventId(String value){googleCalendarEventId=value;} public void setStatus(ConsultationStatus value){status=value;}
}
