package com.cornercircle.backend.media.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class MediaExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(MediaExceptionHandler.class);

    @ExceptionHandler(InvalidMediaException.class)
    ResponseEntity<Map<String, String>> invalidMedia(InvalidMediaException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> uploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "The image is too large. Choose an image smaller than 10 MB."));
    }

    @ExceptionHandler(MediaUploadException.class)
    ResponseEntity<Map<String, String>> uploadFailure(MediaUploadException exception) {
        log.error("Cloudinary image upload failed", exception);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "The image storage service rejected the upload. Check the Cloudinary configuration."));
    }
}
