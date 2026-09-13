package com.cornercircle.backend.newsletter.dto;

import com.cornercircle.backend.newsletter.model.NewsletterStatus;
import com.cornercircle.backend.newsletter.model.NewsletterSyncStatus;
import java.time.LocalDateTime;

public record NewsletterSubscriberResponse(
        Long id,
        String email,
        NewsletterStatus status,
        String source,
        NewsletterSyncStatus resendSyncStatus,
        String resendSyncError,
        LocalDateTime subscribedAt,
        LocalDateTime unsubscribedAt,
        LocalDateTime welcomeEmailSentAt
) {}
