package com.cornercircle.backend.membership.service;

import com.cornercircle.backend.membership.model.MembershipApplication;
import com.cornercircle.backend.membership.model.MembershipStatus;
import com.cornercircle.backend.membership.repository.MembershipApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

@Service
public class MembershipEmailService {
    private final MembershipApplicationRepository applications;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String from;
    private final String replyTo;
    private final String frontendUrl;

    public MembershipEmailService(MembershipApplicationRepository applications, ObjectMapper mapper,
            @Value("${RESEND_API_KEY:}") String apiKey,
            @Value("${MEMBERSHIP_EMAIL_FROM:${EVENT_EMAIL_FROM:}}") String from,
            @Value("${MEMBERSHIP_EMAIL_REPLY_TO:${EVENT_EMAIL_REPLY_TO:}}") String replyTo,
            @Value("${app.frontend-url:${FRONTEND_URL:http://localhost:3000}}") String frontendUrl) {
        this.applications = applications; this.mapper = mapper; this.apiKey = apiKey; this.from = from;
        this.replyTo = replyTo; this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public void sendIfNeeded(MembershipApplication member, boolean renewal) {
        if (member.getStatus() != MembershipStatus.ACTIVE || member.getWelcomeEmailSentAt() != null) return;
        if (apiKey.isBlank() || from.isBlank()) { member.recordWelcomeEmailError("Email delivery is not configured."); applications.save(member); return; }
        try {
            String firstName = member.getFullName().trim().split("\\s+")[0];
            String subject = renewal ? "Your Cornerstone membership has been renewed" : "Welcome to Cornerstone Social Circle";
            String heading = renewal ? "Your membership is renewed" : "Welcome to your circle";
            String dates = format(member.getMembershipStartsOn()) + " – " + format(member.getMembershipEndsOn());
            String html = "<!doctype html><html><body style=\"margin:0;background:#f8f5ef;color:#21162a;font-family:Arial,sans-serif\"><table role=\"presentation\" width=\"100%\"><tr><td align=\"center\" style=\"padding:40px 16px\"><table role=\"presentation\" width=\"600\" style=\"max-width:600px;background:#fffdf9;border:1px solid #d8ccbc\"><tr><td style=\"padding:42px\"><p style=\"color:#9b6a1b;text-transform:uppercase;letter-spacing:2px\">Cornerstone Social Circle</p><h1 style=\"font-family:Georgia,serif;font-size:38px;font-weight:400\">" + heading + "</h1><p style=\"font-size:17px;line-height:1.7\">Hi " + esc(firstName) + ",</p><p style=\"font-size:17px;line-height:1.7\">Your annual Cornerstone Social Circle membership is active. We’re delighted to welcome you into a community built around meaningful connection.</p><div style=\"margin:28px 0;padding:20px;border-top:1px solid #e4dacc;border-bottom:1px solid #e4dacc\"><b>Membership period</b><br>" + dates + "</div><a href=\"" + frontendUrl + "/events\" style=\"display:inline-block;background:#2b1d35;color:white;padding:14px 22px;text-decoration:none;font-weight:bold\">Browse upcoming events</a><p style=\"margin-top:30px;color:#625b64\">Questions? Reply to this email and we’ll be happy to help.</p></td></tr></table></td></tr></table></body></html>";
            String text = heading + "\n\nHi " + firstName + ",\n\nYour annual Cornerstone Social Circle membership is active.\nMembership period: " + dates + "\n\nBrowse events: " + frontendUrl + "/events";
            var payload = new LinkedHashMap<String,Object>(); payload.put("from", from); payload.put("to", new String[]{member.getEmail()}); payload.put("subject", subject); payload.put("html", html); payload.put("text", text); if (!replyTo.isBlank()) payload.put("reply_to", replyTo);
            var request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails")).timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                .header("User-Agent", "Cornerstone-Social-Circle/1.0")
                .header("Idempotency-Key", "membership-" + member.getPublicId() + "-" + member.getMembershipEndsOn())
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8)).build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Resend returned HTTP " + response.statusCode());
            member.recordWelcomeEmailSent();
        } catch (Exception exception) { member.recordWelcomeEmailError(exception.getMessage() == null ? "Email delivery failed." : exception.getMessage()); }
        applications.save(member);
    }

    public void sendRenewalReminder(MembershipApplication member) {
        if (apiKey.isBlank() || from.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email delivery is not configured.");
        if (member.getMembershipEndsOn() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This member does not have a renewal date.");
        try {
            String firstName = member.getFullName().trim().split("\\s+")[0];
            String renewalUrl = frontendUrl + "/membership/success?application_id=" + member.getPublicId();
            String endDate = format(member.getMembershipEndsOn());
            String subject = "Your Cornerstone Social Circle membership renewal";
            String html = "<!doctype html><html><body style=\"margin:0;background:#f8f5ef;color:#21162a;font-family:Arial,sans-serif\"><table role=\"presentation\" width=\"100%\"><tr><td align=\"center\" style=\"padding:40px 16px\"><table role=\"presentation\" width=\"600\" style=\"max-width:600px;background:#fffdf9;border:1px solid #d8ccbc\"><tr><td style=\"padding:42px\"><p style=\"color:#9b6a1b;text-transform:uppercase;letter-spacing:2px\">Cornerstone Social Circle</p><h1 style=\"font-family:Georgia,serif;font-size:38px;font-weight:400\">Continue another year with us</h1><p style=\"font-size:17px;line-height:1.7\">Hi " + esc(firstName) + ",</p><p style=\"font-size:17px;line-height:1.7\">Your annual Cornerstone Social Circle membership ends on <b>" + esc(endDate) + "</b>. We would love to welcome you for another year of meaningful connection.</p><a href=\"" + renewalUrl + "\" style=\"display:inline-block;background:#2b1d35;color:white;padding:14px 22px;text-decoration:none;font-weight:bold\">Renew membership</a><p style=\"margin-top:30px;color:#625b64\">Questions? Reply to this email and we’ll be happy to help.</p></td></tr></table></td></tr></table></body></html>";
            String text = "Hi " + firstName + ",\n\nYour annual Cornerstone Social Circle membership ends on " + endDate + ". We would love to welcome you for another year.\n\nRenew membership: " + renewalUrl;
            var payload = new LinkedHashMap<String,Object>();
            payload.put("from", from);
            payload.put("to", new String[]{member.getEmail()});
            payload.put("subject", subject);
            payload.put("html", html);
            payload.put("text", text);
            if (!replyTo.isBlank()) payload.put("reply_to", replyTo);
            var request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails")).timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                .header("User-Agent", "Cornerstone-Social-Circle/1.0")
                .header("Idempotency-Key", "membership-renewal-reminder-" + member.getPublicId() + "-" + member.getMembershipEndsOn())
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8)).build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("Resend returned HTTP " + response.statusCode());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The renewal reminder could not be sent.", exception);
        }
    }

    private String format(java.time.LocalDate value) { return value == null ? "—" : value.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")); }
    private String esc(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
}
