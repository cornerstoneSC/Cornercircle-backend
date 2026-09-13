package com.cornercircle.backend.newsletter.controller;

import com.cornercircle.backend.newsletter.service.NewsletterWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/newsletter/resend")
public class NewsletterWebhookController {
    private final NewsletterWebhookService service;

    public NewsletterWebhookController(NewsletterWebhookService service) {
        this.service = service;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestHeader("svix-id") String eventId,
            @RequestHeader("svix-timestamp") String timestamp,
            @RequestHeader("svix-signature") String signature,
            @RequestBody String body) {
        service.process(eventId, timestamp, signature, body);
        return ResponseEntity.ok().build();
    }
}
