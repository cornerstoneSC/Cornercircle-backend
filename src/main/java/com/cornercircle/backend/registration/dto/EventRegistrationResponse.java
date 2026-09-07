package com.cornercircle.backend.registration.dto;
import java.util.UUID;
public record EventRegistrationResponse(UUID registrationId,String status,String clientSecret){}
