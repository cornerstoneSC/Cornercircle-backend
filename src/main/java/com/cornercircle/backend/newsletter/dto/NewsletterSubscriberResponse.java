package com.cornercircle.backend.newsletter.dto;
import com.cornercircle.backend.newsletter.model.NewsletterStatus; import java.time.LocalDateTime; import java.util.UUID;
public record NewsletterSubscriberResponse(Long id,String email,NewsletterStatus status,LocalDateTime subscribedAt,LocalDateTime unsubscribedAt,UUID unsubscribeToken){}
