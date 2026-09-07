package com.cornercircle.backend.events.dto;

public record EventImageUploadResponse(
        String url,
        String publicId
) {
}
