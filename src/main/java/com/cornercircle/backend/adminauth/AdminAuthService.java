package com.cornercircle.backend.adminauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
public class AdminAuthService {
    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);
    private final AdminCredentialRepository credentials;
    private final ObjectMapper mapper;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(10);
    private final SecureRandom random = new SecureRandom();
    private final String bootstrapUsername, bootstrapPassword, adminEmail, resendKey, emailFrom, frontendUrl;

    public AdminAuthService(AdminCredentialRepository credentials, ObjectMapper mapper,
            @Value("${ADMIN_USERNAME:}") String username, @Value("${ADMIN_PASSWORD:}") String password,
            @Value("${ADMIN_EMAIL:${ADMIN_USERNAME:}}") String adminEmail,
            @Value("${RESEND_API_KEY:}") String resendKey,
            @Value("${PASSWORD_RESET_EMAIL_FROM:${EVENT_EMAIL_FROM:}}") String emailFrom,
            @Value("${FRONTEND_URL:http://localhost:3000}") String frontendUrl) {
        this.credentials=credentials; this.mapper=mapper; this.bootstrapUsername=username.trim(); this.bootstrapPassword=password;
        this.adminEmail=adminEmail.trim().toLowerCase(Locale.ROOT); this.resendKey=resendKey; this.emailFrom=emailFrom;
        this.frontendUrl=frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public AuthenticationResult authenticate(String username, String password) {
        ensureBootstrap();
        AdminCredential credential = credentials.findByUsernameIgnoreCase(clean(username)).orElseGet(() -> credentials.findByEmailIgnoreCase(clean(username)).orElse(null));
        if(credential==null&&credentials.count()==0)return AuthenticationResult.NOT_CONFIGURED;
        if(credential==null||!credential.isActive()||!passwords.matches(password==null?"":password,credential.getPasswordHash()))return AuthenticationResult.REJECTED;
        credential.recordSignIn(); credentials.save(credential); return AuthenticationResult.AUTHENTICATED;
    }

    @Transactional
    public void requestReset(String suppliedEmail) {
        ensureBootstrap();
        AdminCredential credential = credentials.findByEmailIgnoreCase(clean(suppliedEmail)).filter(AdminCredential::isActive).orElse(null);
        if (credential == null) return;
        byte[] bytes = new byte[32]; random.nextBytes(bytes); String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        credential.issueReset(hash(token), LocalDateTime.now().plusMinutes(30)); credentials.save(credential);
        try { sendResetEmail(credential.getEmail(),token); }
        catch (RuntimeException exception) { log.error("Unable to deliver administrator password reset email", exception); }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 12 || newPassword.length() > 128) return false;
        AdminCredential credential = credentials.findByResetTokenHash(hash(token)).orElse(null);
        if (credential == null || credential.getResetTokenHash() == null || credential.getResetTokenExpiresAt() == null
                || credential.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) return false;
        if (!MessageDigest.isEqual(credential.getResetTokenHash().getBytes(StandardCharsets.UTF_8), hash(token).getBytes(StandardCharsets.UTF_8))) return false;
        credential.changePassword(passwords.encode(newPassword)); credentials.save(credential); return true;
    }

    @Transactional public List<AdminSummary> list(){ensureBootstrap();return credentials.findAll().stream().sorted(Comparator.comparing(AdminCredential::getDisplayName,String.CASE_INSENSITIVE_ORDER)).map(AdminSummary::from).toList();}
    @Transactional public AdminSummary add(String name,String email){String cleanEmail=clean(email).toLowerCase(Locale.ROOT),cleanName=clean(name);if(cleanName.isBlank()||!cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||credentials.findByEmailIgnoreCase(cleanEmail).isPresent())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Enter a unique administrator name and email.");long id=credentials.findAll().stream().mapToLong(AdminCredential::getId).max().orElse(0)+1;var admin=credentials.save(new AdminCredential(id,cleanName,cleanEmail,passwords.encode(UUID.randomUUID().toString()+UUID.randomUUID())));requestReset(cleanEmail);return AdminSummary.from(admin);}
    @Transactional public AdminSummary setActive(Long id,boolean active){var admin=credentials.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));if(!active&&admin.isActive()&&credentials.countByActiveTrue()<=1)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"The final active administrator cannot be disabled.");admin.setActive(active);return AdminSummary.from(credentials.save(admin));}
    @Transactional public void resetFor(Long id){var admin=credentials.findById(id).orElseThrow();requestReset(admin.getEmail());}
    private void ensureBootstrap() {
        if(credentials.count()>0){credentials.findById(1L).ifPresent(value->{value.repairProfile("Admin User",adminEmail.isBlank()?bootstrapUsername:adminEmail);credentials.save(value);});return;}
        if (bootstrapUsername.isBlank() || bootstrapPassword.length() < 8) return;
        String email=adminEmail.isBlank()?bootstrapUsername:adminEmail; credentials.save(new AdminCredential(1L,"Admin User",email.toLowerCase(Locale.ROOT),passwords.encode(bootstrapPassword)));
    }

    private void sendResetEmail(String recipient,String token) {
        if (resendKey.isBlank() || emailFrom.isBlank()) throw new IllegalStateException("Password reset email is not configured.");
        try {
            String url=frontendUrl+"/admin-reset-password?token="+java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);
            var payload=new LinkedHashMap<String,Object>(); payload.put("from",emailFrom); payload.put("to",List.of(recipient));
            payload.put("subject","Reset your Cornerstone admin password");
            payload.put("html","<p>A password reset was requested for your Cornerstone administrator account.</p><p><a href=\""+url+"\">Reset password</a></p><p>This link expires in 30 minutes. If you did not request it, ignore this email.</p>");
            var request=HttpRequest.newBuilder(URI.create("https://api.resend.com/emails")).timeout(Duration.ofSeconds(12)).header("Authorization","Bearer "+resendKey).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
            var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.discarding());
            if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("Resend returned HTTP "+response.statusCode());
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException("Password reset email was interrupted."); }
        catch (Exception exception) { throw new IllegalStateException("Password reset email could not be sent."); }
    }

    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((value==null?"":value).getBytes(StandardCharsets.UTF_8))); } catch(Exception e){throw new IllegalStateException(e);} }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    public record AdminSummary(Long id,String displayName,String email,LocalDateTime lastSignInAt,boolean active){static AdminSummary from(AdminCredential value){return new AdminSummary(value.getId(),value.getDisplayName(),value.getEmail(),value.getLastSignInAt(),value.isActive());}}
    public enum AuthenticationResult { AUTHENTICATED, REJECTED, NOT_CONFIGURED }
}
