package com.cornercircle.backend.events.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_events_slug",
                        columnNames = "slug"
                )
        }
)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------------------------------
    // EVENT BASICS
    // -------------------------------------------------

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(length = 60)
    private String category;

    @Column(name = "short_description", length = 300)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    // -------------------------------------------------
    // DATE & TIME
    // -------------------------------------------------

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "time_zone", length = 100)
    private String timeZone = "America/Los_Angeles";

    // -------------------------------------------------
    // LOCATION
    // -------------------------------------------------

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Column(length = 300)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    // -------------------------------------------------
    // WHAT TO EXPECT
    // -------------------------------------------------

    @ElementCollection
    @CollectionTable(
            name = "event_expectations",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @OrderColumn(name = "display_order")
    private List<EventExpectation> expectations = new ArrayList<>();

    // -------------------------------------------------
    // ADDITIONAL INFORMATION
    // -------------------------------------------------

    @Column(
            name = "additional_information",
            columnDefinition = "TEXT"
    )
    private String additionalInformation;

    // -------------------------------------------------
    // REGISTRATION
    // -------------------------------------------------

    @Column(
            name = "price_per_person",
            precision = 10,
            scale = 2
    )
    private BigDecimal pricePerPerson;

    private Integer capacity;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    // -------------------------------------------------
    // PUBLICATION
    // -------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(
            name = "publication_status",
            nullable = false,
            length = 20
    )
    private EventPublicationStatus publicationStatus =
            EventPublicationStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private EventVisibility visibility =
            EventVisibility.HIDDEN;

    // -------------------------------------------------
    // AUDIT
    // -------------------------------------------------

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    public Event() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // -------------------------------------------------
    // GETTERS / SETTERS
    // -------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public List<EventExpectation> getExpectations() {
        return expectations;
    }

    public void setExpectations(
            List<EventExpectation> expectations
    ) {
        this.expectations = expectations;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(
            String additionalInformation
    ) {
        this.additionalInformation = additionalInformation;
    }

    public BigDecimal getPricePerPerson() {
        return pricePerPerson;
    }

    public void setPricePerPerson(BigDecimal pricePerPerson) {
        this.pricePerPerson = pricePerPerson;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(
            LocalDateTime registrationDeadline
    ) {
        this.registrationDeadline = registrationDeadline;
    }

    public EventPublicationStatus getPublicationStatus() {
        return publicationStatus;
    }

    public void setPublicationStatus(
            EventPublicationStatus publicationStatus
    ) {
        this.publicationStatus = publicationStatus;
    }

    public EventVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(
            EventVisibility visibility
    ) {
        this.visibility = visibility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
