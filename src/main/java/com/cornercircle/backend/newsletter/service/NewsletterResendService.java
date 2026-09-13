package com.cornercircle.backend.newsletter.service;

import com.cornercircle.backend.newsletter.model.NewsletterSubscriber;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NewsletterResendService {
    private static final URI CONTACTS_URI = URI.create("https://api.resend.com/contacts");
    private static final URI EMAILS_URI = URI.create("https://api.resend.com/emails");
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String segmentId;
    private final String from;
    private final String replyTo;
    private final String frontendUrl;

    public NewsletterResendService(ObjectMapper objectMapper,
            @Value("${RESEND_API_KEY:}") String apiKey,
            @Value("${RESEND_NEWSLETTER_SEGMENT_ID:}") String segmentId,
            @Value("${NEWSLETTER_EMAIL_FROM:}") String from,
            @Value("${NEWSLETTER_EMAIL_REPLY_TO:}") String replyTo,
            @Value("${FRONTEND_URL:https://www.cornerstonesocialcircle.com}") String frontendUrl) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.segmentId = segmentId;
        this.from = from;
        this.replyTo = replyTo;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public String syncContact(NewsletterSubscriber subscriber, boolean unsubscribed) throws Exception {
        requireConfigured();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", subscriber.getEmail());
        payload.put("unsubscribed", unsubscribed);
        if (!segmentId.isBlank() && !unsubscribed) payload.put("segments", List.of(Map.of("id", segmentId)));
        HttpResponse<String> response = send(CONTACTS_URI, "POST", payload);
        if (response.statusCode() == 409) {
            URI updateUri = URI.create(CONTACTS_URI + "/" + URLEncoder.encode(subscriber.getEmail(), StandardCharsets.UTF_8));
            response = send(updateUri, "PATCH", Map.of("unsubscribed", unsubscribed));
        }
        ensureSuccess(response, "contact synchronization");
        JsonNode body = response.body().isBlank() ? null : objectMapper.readTree(response.body());
        return body == null ? null : body.path("id").asText(null);
    }

    public void sendWelcomeEmail(NewsletterSubscriber subscriber) throws Exception {
        requireConfigured();
        if (from.isBlank()) throw new IllegalStateException("NEWSLETTER_EMAIL_FROM is not configured");
        String unsubscribeUrl = frontendUrl + "/newsletter/unsubscribe?token=" + subscriber.getUnsubscribeToken();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", from);
        payload.put("to", List.of(subscriber.getEmail()));
        if (!replyTo.isBlank()) payload.put("reply_to", replyTo);
        payload.put("subject", "Welcome to Cornerstone Social Circle");
        payload.put("html", welcomeHtml(unsubscribeUrl));
        payload.put("headers", Map.of(
                "List-Unsubscribe", "<" + unsubscribeUrl + ">",
                "List-Unsubscribe-Post", "List-Unsubscribe=One-Click"));
        ensureSuccess(send(EMAILS_URI, "POST", payload), "welcome email");
    }

    private HttpResponse<String> send(URI uri, String method, Map<String, Object> payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload))).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void ensureSuccess(HttpResponse<String> response, String operation) {
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalStateException("Resend " + operation + " returned HTTP " + response.statusCode());
    }

    private void requireConfigured() {
        if (apiKey.isBlank()) throw new IllegalStateException("RESEND_API_KEY is not configured");
    }

    private String welcomeHtml(String unsubscribeUrl) {
        return """
                <!doctype html><html><body style="margin:0;background:#f7f3ec;color:#332b31;font-family:Georgia,serif">
                <div style="max-width:620px;margin:auto;padding:48px 28px">
                  <p style="color:#a97420;font:700 12px Arial,sans-serif;letter-spacing:.2em;text-transform:uppercase">Cornerstone Social Circle</p>
                  <h1 style="font-size:38px;font-weight:400;line-height:1.15">Welcome to the Circle</h1>
                  <p style="font-size:17px;line-height:1.75">We’re delighted to welcome you to Cornerstone Social Circle—a community created for meaningful conversations, genuine friendships, and a stronger sense of belonging.</p>
                  <p style="font-size:17px;line-height:1.75">You’ll be among the first to hear about upcoming gatherings, membership opportunities, and thoughtful community updates.</p>
                  <p style="margin:34px 0"><a href="%s" style="background:#6f7968;color:white;padding:14px 22px;text-decoration:none;font:700 13px Arial,sans-serif;letter-spacing:.08em">EXPLORE UPCOMING EVENTS</a></p>
                  <p style="font-size:17px;line-height:1.75">Warmly,<br><strong>Dr. Eya Touglo</strong><br>Founder, Cornerstone Social Circle</p>
                  <p style="margin-top:42px;font:12px/1.6 Arial,sans-serif;color:#777">You received this email because you subscribed at cornerstonesocialcircle.com. <a href="%s" style="color:#777">Unsubscribe</a>.</p>
                </div></body></html>
                """.formatted(frontendUrl + "/events", unsubscribeUrl);
    }
}
