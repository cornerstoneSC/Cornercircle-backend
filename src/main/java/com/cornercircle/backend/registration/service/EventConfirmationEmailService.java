package com.cornercircle.backend.registration.service;

import com.cornercircle.backend.registration.model.EventRegistration;
import com.cornercircle.backend.registration.model.EventRegistrationStatus;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EventConfirmationEmailService {
    private final EventRegistrationRepository registrations;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String from;
    private final String replyTo;
    private final String frontendUrl;

    public EventConfirmationEmailService(EventRegistrationRepository registrations, ObjectMapper mapper,
            @Value("${RESEND_API_KEY:}") String apiKey,
            @Value("${EVENT_EMAIL_FROM:}") String from,
            @Value("${EVENT_EMAIL_REPLY_TO:}") String replyTo,
            @Value("${app.frontend-url:${FRONTEND_URL:http://localhost:3000}}") String frontendUrl) {
        this.registrations = registrations;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.from = from;
        this.replyTo = replyTo;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public void sendIfNeeded(EventRegistration registration) {
        if (registration.getStatus() != EventRegistrationStatus.CONFIRMED || registration.getConfirmationEmailSentAt() != null) return;
        if (apiKey.isBlank() || from.isBlank()) {
            registration.recordConfirmationEmailError("Email delivery is not configured.");
            registrations.save(registration);
            return;
        }
        try {
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("from", from);
            payload.put("to", new String[]{registration.getEmail()});
            payload.put("subject", "Your place is confirmed — " + registration.getEvent().getTitle());
            payload.put("html", html(registration));
            payload.put("text", text(registration));
            if (!replyTo.isBlank()) payload.put("reply_to", replyTo);
            var request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Cornerstone-Social-Circle/1.0")
                    .header("Idempotency-Key", "event-confirmation-" + registration.getPublicId())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("Resend returned HTTP " + response.statusCode());
            registration.recordConfirmationEmailSent();
        } catch (Exception exception) {
            registration.recordConfirmationEmailError(exception.getMessage() == null ? "Email delivery failed." : exception.getMessage());
        }
        registrations.save(registration);
    }

    private String html(EventRegistration r) {
        var event = r.getEvent();
        String base = frontendUrl + "/api/events/" + enc(event.getSlug()) + "/registrations/" + r.getPublicId();
        String details = frontendUrl + "/events/" + enc(event.getSlug());
        String cityStateZip = java.util.stream.Stream.of(event.getCity(), event.getState(), event.getZipCode())
                .filter(value -> !blank(value)).collect(java.util.stream.Collectors.joining(" "));
        String address = join(event.getVenueName(), event.getAddress(), cityStateZip);
        String date = event.getEventDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US));
        String time = event.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a")) + "–" + event.getEndTime().format(DateTimeFormatter.ofPattern("h:mm a"));
        return "<!doctype html><html><body style=\"margin:0;background:#f8f5ef;color:#21162a;font-family:Arial,sans-serif\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:40px 16px\"><table role=\"presentation\" width=\"620\" style=\"max-width:620px;background:#fffdf9;border:1px solid #d8ccbc\"><tr><td align=\"center\" style=\"padding:34px 28px 12px\"><div style=\"width:42px;height:42px;line-height:42px;border-radius:50%;background:#c28a2c;color:white;font-size:24px\">✓</div><h1 style=\"font-family:Georgia,serif;font-weight:400;font-size:34px;margin:16px 0 5px\">Your place is reserved</h1></td></tr><tr><td style=\"padding:22px 36px\"><h2 style=\"font-family:Georgia,serif;font-size:30px;font-weight:400;margin:0 0 22px\">" + esc(event.getTitle()) + "</h2><p style=\"line-height:1.7\"><b>" + esc(date) + "</b><br>" + esc(time) + "</p><p style=\"line-height:1.6\">" + esc(address).replace(" | ", "<br>") + "</p><hr style=\"border:0;border-top:1px solid #e4dacc;margin:24px 0\"><p><b>" + esc(r.getFullName()) + "</b><br>" + r.getGuestCount() + (r.getGuestCount() == 1 ? " attendee" : " attendees") + "</p><p>Confirmation <b>" + confirmation(r) + "</b></p></td></tr><tr><td align=\"center\" style=\"padding:8px 36px 30px\"><img src=\"" + base + "/ticket.png\" width=\"230\" height=\"230\" alt=\"Event check-in QR code\" style=\"display:block;border:8px solid white\"><p style=\"color:#625b64\">Present this QR code at check-in</p><a href=\"" + base + "/calendar.ics\" style=\"display:inline-block;padding:14px 22px;border:1px solid #b47d22;color:#6f4b13;text-decoration:none;font-weight:bold\">Add to Calendar</a><p><a href=\"" + details + "\" style=\"color:#21162a\">View event details</a></p></td></tr></table></td></tr></table></body></html>";
    }

    private String text(EventRegistration r) {
        var event = r.getEvent();
        return "YOUR PLACE IS RESERVED\n\n" + event.getTitle() + "\n" + event.getEventDate() + "\n" + event.getStartTime() + "–" + event.getEndTime() + "\n" + join(event.getVenueName(), event.getAddress(), join(event.getCity(), event.getState(), event.getZipCode())).replace(" | ", "\n") + "\n\n" + r.getFullName() + "\n" + r.getGuestCount() + " attendee(s)\nConfirmation " + confirmation(r) + "\n\nView your ticket: " + frontendUrl + "/events/" + enc(event.getSlug()) + "/register/success?registration_id=" + r.getPublicId();
    }

    private String confirmation(EventRegistration r) { return "CSC-" + r.getPublicId().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT); }
    private String enc(String value) { return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String join(String... values) { return java.util.Arrays.stream(values).filter(v -> !blank(v)).collect(java.util.stream.Collectors.joining(" | ")); }
    private String esc(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
}
