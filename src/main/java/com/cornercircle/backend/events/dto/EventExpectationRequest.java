package com.cornercircle.backend.events.dto;

import jakarta.validation.constraints.Size;

public class EventExpectationRequest {

    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    public EventExpectationRequest() {
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