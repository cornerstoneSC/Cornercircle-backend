package com.cornercircle.backend.newsletter.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "newsletter_subscribers", uniqueConstraints = @UniqueConstraint(name = "uk_newsletter_email", columnNames = "email"))
public class NewsletterSubscriber {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 320) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private NewsletterStatus status;
    @Column(name = "unsubscribe_token", nullable = false, unique = true) private UUID unsubscribeToken;
    @Column(name = "subscribed_at", nullable = false) private LocalDateTime subscribedAt;
    @Column(name = "unsubscribed_at") private LocalDateTime unsubscribedAt;
    @Column(length = 120) private String source;
    @Column(name = "resend_contact_id", length = 100) private String resendContactId;
    @Enumerated(EnumType.STRING) @Column(name = "resend_sync_status", length = 20) private NewsletterSyncStatus resendSyncStatus;
    @Column(name = "resend_sync_error", length = 500) private String resendSyncError;
    @Column(name = "welcome_email_sent_at") private LocalDateTime welcomeEmailSentAt;
    @Column(name = "welcome_email_error", length = 500) private String welcomeEmailError;

    protected NewsletterSubscriber() {}

    public NewsletterSubscriber(String email, String source) {
        this.email = email;
        this.source = source;
        status = NewsletterStatus.ACTIVE;
        unsubscribeToken = UUID.randomUUID();
        subscribedAt = LocalDateTime.now();
        resendSyncStatus = NewsletterSyncStatus.PENDING;
    }

    public void reactivate(String source) {
        status = NewsletterStatus.ACTIVE;
        subscribedAt = LocalDateTime.now();
        unsubscribedAt = null;
        unsubscribeToken = UUID.randomUUID();
        this.source = source;
        resendSyncStatus = NewsletterSyncStatus.PENDING;
        resendSyncError = null;
    }

    public void unsubscribe() {
        status = NewsletterStatus.UNSUBSCRIBED;
        unsubscribedAt = LocalDateTime.now();
        resendSyncStatus = NewsletterSyncStatus.PENDING;
        resendSyncError = null;
    }

    public void applyResendStatus(boolean unsubscribed) {
        if (unsubscribed) {
            status = NewsletterStatus.UNSUBSCRIBED;
            if (unsubscribedAt == null) unsubscribedAt = LocalDateTime.now();
        } else {
            status = NewsletterStatus.ACTIVE;
            unsubscribedAt = null;
        }
        resendSyncStatus = NewsletterSyncStatus.SYNCED;
        resendSyncError = null;
    }

    public void markResendSynced(String contactId) {
        if (contactId != null && !contactId.isBlank()) resendContactId = contactId;
        resendSyncStatus = NewsletterSyncStatus.SYNCED;
        resendSyncError = null;
    }

    public void markResendFailed(String message) {
        resendSyncStatus = NewsletterSyncStatus.FAILED;
        resendSyncError = truncate(message);
    }

    public void markWelcomeEmailSent() {
        welcomeEmailSentAt = LocalDateTime.now();
        welcomeEmailError = null;
    }

    public void markWelcomeEmailFailed(String message) {
        welcomeEmailError = truncate(message);
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public NewsletterStatus getStatus() { return status; }
    public UUID getUnsubscribeToken() { return unsubscribeToken; }
    public LocalDateTime getSubscribedAt() { return subscribedAt; }
    public LocalDateTime getUnsubscribedAt() { return unsubscribedAt; }
    public String getSource() { return source; }
    public NewsletterSyncStatus getResendSyncStatus() { return resendSyncStatus == null ? NewsletterSyncStatus.PENDING : resendSyncStatus; }
    public String getResendSyncError() { return resendSyncError; }
    public LocalDateTime getWelcomeEmailSentAt() { return welcomeEmailSentAt; }
}
