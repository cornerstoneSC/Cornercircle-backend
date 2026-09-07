package com.cornercircle.backend.membership.controller;

import com.cornercircle.backend.membership.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class StripeWebhookController {
    private final StripeWebhookService service;
    private final String webhookSecret;
    public StripeWebhookController(StripeWebhookService service, @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.service = service;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping(value = "/api/v1/stripe/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> webhook(@RequestBody String rawBody, @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (!webhookSecret.startsWith("whsec_")) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        if (signature == null || signature.isBlank()) return ResponseEntity.badRequest().build();
        try {
            Event event = Webhook.constructEvent(rawBody, signature, webhookSecret);
            service.process(event.getId(), event.getType(), rawBody);
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException exception) {
            return ResponseEntity.badRequest().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
