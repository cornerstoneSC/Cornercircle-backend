package com.cornercircle.backend.membership.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = MembershipController.class)
public class MembershipValidationExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> membershipStatusError(ResponseStatusException exception) {
        String message = exception.getReason() == null ? "The membership request could not be completed." : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ValidationErrorResponse> invalidApplication(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.putIfAbsent(error.getField(), message(error.getField(), error.getDefaultMessage()))
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ValidationErrorResponse("Please correct the highlighted membership fields.", fieldErrors));
    }

    private static String message(String field, String fallback) {
        return switch (field) {
            case "fullName" -> "Enter your full name using 160 characters or fewer.";
            case "email" -> "Enter a valid email address.";
            case "phone" -> "Use 40 characters or fewer.";
            case "city" -> "Enter your city using 120 characters or fewer.";
            case "birthday" -> "Birthday must be before today.";
            case "inspiredBy" -> "Tell us what inspired you to join using 3,000 characters or fewer.";
            case "activities" -> "Choose at least one activity.";
            case "goals" -> "Choose at least one goal.";
            case "membershipAgreementAccepted" -> "Accept the membership agreement to continue.";
            case "photographyNoticeAcknowledged" -> "Acknowledge the photography notice to continue.";
            case "comments" -> "Use 3,000 characters or fewer.";
            default -> fallback == null ? "This value is invalid." : fallback;
        };
    }

    record ValidationErrorResponse(String message, Map<String, String> fieldErrors) {}
}
