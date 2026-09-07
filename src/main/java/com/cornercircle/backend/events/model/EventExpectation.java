package com.cornercircle.backend.events.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EventExpectation {

    @Column(name = "expectation_title", length = 100)
    private String title;

    @Column(name = "expectation_description", length = 500)
    private String description;

    public EventExpectation() {
    }

    public EventExpectation(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}