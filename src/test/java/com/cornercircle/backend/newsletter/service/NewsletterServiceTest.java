package com.cornercircle.backend.newsletter.service;

import com.cornercircle.backend.newsletter.model.NewsletterStatus;
import com.cornercircle.backend.newsletter.model.NewsletterSubscribeResult;
import com.cornercircle.backend.newsletter.model.NewsletterSubscriber;
import com.cornercircle.backend.newsletter.model.NewsletterSyncStatus;
import com.cornercircle.backend.newsletter.repository.NewsletterSubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NewsletterServiceTest {
    private NewsletterSubscriberRepository repository;
    private NewsletterResendService resend;
    private NewsletterService service;

    @BeforeEach
    void setUp() {
        repository = mock(NewsletterSubscriberRepository.class);
        resend = mock(NewsletterResendService.class);
        service = new NewsletterService(repository, resend);
    }

    @Test
    void createsAndSynchronizesNewSubscriber() throws Exception {
        when(repository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
        when(resend.syncContact(any(), eq(false))).thenReturn("contact-1");

        NewsletterSubscribeResult result = service.subscribe(" Person@Example.com ", "/events");

        assertEquals(NewsletterSubscribeResult.NEW_SUBSCRIBER, result);
        verify(repository).save(any(NewsletterSubscriber.class));
        verify(resend).sendWelcomeEmail(any(NewsletterSubscriber.class));
    }

    @Test
    void activeDuplicateDoesNotSendAnotherWelcomeEmail() throws Exception {
        NewsletterSubscriber subscriber = new NewsletterSubscriber("person@example.com", "/");
        when(repository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(subscriber));

        assertEquals(NewsletterSubscribeResult.ALREADY_SUBSCRIBED,
                service.subscribe("person@example.com", "/events"));
        verifyNoInteractions(resend);
    }

    @Test
    void reactivatesUnsubscribedSubscriber() throws Exception {
        NewsletterSubscriber subscriber = new NewsletterSubscriber("person@example.com", "/");
        subscriber.unsubscribe();
        when(repository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(subscriber));

        assertEquals(NewsletterSubscribeResult.REACTIVATED,
                service.subscribe("person@example.com", "/events"));
        assertEquals(NewsletterStatus.ACTIVE, subscriber.getStatus());
        verify(resend).syncContact(subscriber, false);
        verify(resend).sendWelcomeEmail(subscriber);
    }

    @Test
    void keepsSubscriberWhenResendIsUnavailable() throws Exception {
        when(repository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("unavailable")).when(resend).syncContact(any(), eq(false));
        doThrow(new IllegalStateException("unavailable")).when(resend).sendWelcomeEmail(any());

        assertEquals(NewsletterSubscribeResult.NEW_SUBSCRIBER,
                service.subscribe("person@example.com", "/"));
        var subscriber = org.mockito.ArgumentCaptor.forClass(NewsletterSubscriber.class);
        verify(repository).save(subscriber.capture());
        assertEquals(NewsletterSyncStatus.FAILED, subscriber.getValue().getResendSyncStatus());
    }

    @Test
    void rejectsInvalidEmail() {
        assertThrows(ResponseStatusException.class, () -> service.subscribe("invalid", "/"));
        verifyNoInteractions(repository, resend);
    }
}
