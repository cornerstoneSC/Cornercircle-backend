package com.cornercircle.backend.newsletter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class NewsletterWebhookService {
    private static final long MAX_AGE_SECONDS = 300;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final NewsletterService newsletterService;
    private final String signingSecret;

    public NewsletterWebhookService(ObjectMapper objectMapper, JdbcTemplate jdbcTemplate,
            NewsletterService newsletterService, @Value("${RESEND_WEBHOOK_SECRET:}") String signingSecret) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.newsletterService = newsletterService;
        this.signingSecret = signingSecret;
    }

    @Transactional
    public void process(String eventId, String timestamp, String signature, String body) {
        verify(eventId, timestamp, signature, body);
        int inserted = jdbcTemplate.update(
                "INSERT INTO newsletter_webhook_events (event_id, processed_at) VALUES (?, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",
                eventId);
        if (inserted == 0) return;
        try {
            JsonNode event = objectMapper.readTree(body);
            if (!"contact.updated".equals(event.path("type").asText())) return;
            JsonNode data = event.path("data");
            String email = data.path("email").asText("").trim();
            if (!email.isBlank() && data.has("unsubscribed"))
                newsletterService.applyResendStatus(email, data.path("unsubscribed").asBoolean());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload");
        }
    }

    private void verify(String eventId, String timestamp, String signature, String body) {
        if (signingSecret.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Newsletter webhook is not configured");
        try {
            long sentAt = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - sentAt) > MAX_AGE_SECONDS)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired webhook signature");
            String encodedSecret = signingSecret.startsWith("whsec_") ? signingSecret.substring(6) : signingSecret;
            byte[] secret;
            try { secret = Base64.getDecoder().decode(encodedSecret); }
            catch (IllegalArgumentException ignored) { secret = Base64.getUrlDecoder().decode(encodedSecret); }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] expected = mac.doFinal((eventId + "." + timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            boolean valid = false;
            for (String candidate : signature.split("\\s+")) {
                if (!candidate.startsWith("v1,")) continue;
                byte[] supplied = Base64.getDecoder().decode(candidate.substring(3));
                if (MessageDigest.isEqual(expected, supplied)) { valid = true; break; }
            }
            if (!valid) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
    }
}
