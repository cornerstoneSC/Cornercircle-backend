package com.cornercircle.backend.events.dto;

import com.cornercircle.backend.events.model.EventPublicationStatus;
import com.cornercircle.backend.events.model.EventVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class EventRequest {

    @NotBlank(message = "Event title is required")
    @Size(max = 150)
    private String title;

    @Size(max = 60)
    private String category;

    @Size(max = 300)
    private String shortDescription;

    private String description;

    @Size(max = 1000)
    private String coverImageUrl;

    private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String timeZone = "America/Los_Angeles";

    private String venueName;

    private String address;

    private String city;

    private String state;

    private String zipCode;

    @Valid
    private List<EventExpectationRequest> expectations =
            new ArrayList<>();

    private String additionalInformation;

    private BigDecimal pricePerPerson;

    private Integer capacity;

    private LocalDateTime registrationDeadline;

    private EventPublicationStatus publicationStatus =
            EventPublicationStatus.DRAFT;

    private EventVisibility visibility =
            EventVisibility.HIDDEN;

    public EventRequest() {
    }

    // getters / setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public List<EventExpectationRequest> getExpectations() {
        return expectations;
    }

    public void setExpectations(
            List<EventExpectationRequest> expectations
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

    public void setVisibility(EventVisibility visibility) {
        this.visibility = visibility;
    }
}
