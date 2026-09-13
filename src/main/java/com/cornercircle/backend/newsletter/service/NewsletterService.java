package com.cornercircle.backend.newsletter.service;

import com.cornercircle.backend.newsletter.dto.NewsletterSubscriberResponse;
import com.cornercircle.backend.newsletter.model.*;
import com.cornercircle.backend.newsletter.repository.NewsletterSubscriberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class NewsletterService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final NewsletterSubscriberRepository repository;
    private final NewsletterResendService resendService;

    public NewsletterService(NewsletterSubscriberRepository repository, NewsletterResendService resendService) {
        this.repository = repository;
        this.resendService = resendService;
    }

    @Transactional
    public NewsletterSubscribeResult subscribe(String rawEmail, String rawSource) {
        String email = normalizeEmail(rawEmail);
        String source = normalizeSource(rawSource);
        var existing = repository.findByEmailIgnoreCase(email);
        if (existing.isPresent() && existing.get().getStatus() == NewsletterStatus.ACTIVE)
            return NewsletterSubscribeResult.ALREADY_SUBSCRIBED;

        NewsletterSubscriber subscriber;
        NewsletterSubscribeResult result;
        if (existing.isPresent()) {
            subscriber = existing.get();
            subscriber.reactivate(source);
            result = NewsletterSubscribeResult.REACTIVATED;
        } else {
            subscriber = new NewsletterSubscriber(email, source);
            repository.save(subscriber);
            result = NewsletterSubscribeResult.NEW_SUBSCRIBER;
        }
        synchronize(subscriber, false);
        sendWelcome(subscriber);
        return result;
    }

    @Transactional
    public void unsubscribe(UUID token) {
        NewsletterSubscriber subscriber = repository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This unsubscribe link is invalid."));
        if (subscriber.getStatus() != NewsletterStatus.UNSUBSCRIBED) subscriber.unsubscribe();
        synchronize(subscriber, true);
    }

    @Transactional
    public void applyResendStatus(String email, boolean unsubscribed) {
        repository.findByEmailIgnoreCase(email).ifPresent(s -> s.applyResendStatus(unsubscribed));
    }

    @Transactional
    public NewsletterSubscriberResponse retry(Long id) {
        NewsletterSubscriber subscriber = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscriber not found."));
        synchronize(subscriber, subscriber.getStatus() == NewsletterStatus.UNSUBSCRIBED);
        if (subscriber.getStatus() == NewsletterStatus.ACTIVE && subscriber.getWelcomeEmailSentAt() == null)
            sendWelcome(subscriber);
        return toResponse(subscriber);
    }

    @Transactional(readOnly = true)
    public List<NewsletterSubscriberResponse> list(String query, NewsletterStatus status) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> needle.isBlank() || s.getEmail().contains(needle))
                .sorted((a, b) -> b.getSubscribedAt().compareTo(a.getSubscribedAt()))
                .map(this::toResponse)
                .toList();
    }

    private NewsletterSubscriberResponse toResponse(NewsletterSubscriber s) {
        return new NewsletterSubscriberResponse(s.getId(), s.getEmail(), s.getStatus(), s.getSource(),
                s.getResendSyncStatus(), s.getResendSyncError(), s.getSubscribedAt(),
                s.getUnsubscribedAt(), s.getWelcomeEmailSentAt());
    }

    private void synchronize(NewsletterSubscriber subscriber, boolean unsubscribed) {
        try {
            subscriber.markResendSynced(resendService.syncContact(subscriber, unsubscribed));
        } catch (Exception exception) {
            subscriber.markResendFailed(exception.getMessage());
        }
    }

    private void sendWelcome(NewsletterSubscriber subscriber) {
        try {
            resendService.sendWelcomeEmail(subscriber);
            subscriber.markWelcomeEmailSent();
        } catch (Exception exception) {
            subscriber.markWelcomeEmailFailed(exception.getMessage());
        }
    }

    private String normalizeEmail(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 320 || !EMAIL.matcher(email).matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        return email;
    }

    private String normalizeSource(String raw) {
        String source = raw == null || raw.isBlank() ? "website" : raw.trim();
        return source.length() <= 120 ? source : source.substring(0, 120);
    }
}
