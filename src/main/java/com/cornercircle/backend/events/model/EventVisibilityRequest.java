package com.cornercircle.backend.events.dto;

import com.cornercircle.backend.events.model.EventVisibility;
import jakarta.validation.constraints.NotNull;

public class EventVisibilityRequest {

    @NotNull(message = "Visibility is required")
    private EventVisibility visibility;

    public EventVisibilityRequest() {
    }

    public EventVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(EventVisibility visibility) {
        this.visibility = visibility;
    }
}

