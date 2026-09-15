package com.cornercircle.backend.adminauth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_credentials")
public class AdminCredential {
    @Id private Long id;
    @Column(nullable = false, unique = true, length = 254) private String username;
    @Column(name = "display_name", nullable = false, length = 160) private String displayName;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "last_sign_in_at") private LocalDateTime lastSignInAt;
    @Column(name = "reset_token_hash", length = 64) private String resetTokenHash;
    @Column(name = "reset_token_expires_at") private LocalDateTime resetTokenExpiresAt;
    @Column(name = "password_changed_at") private LocalDateTime passwordChangedAt;

    protected AdminCredential() {}
    public AdminCredential(Long id, String displayName, String email, String passwordHash) { this.id=id; this.displayName=displayName; this.email=email; this.username=email; this.passwordHash=passwordHash; this.active=true; }
    public Long getId(){return id;} public String getDisplayName(){return displayName;} public String getEmail(){return email;} public boolean isActive(){return active;} public LocalDateTime getLastSignInAt(){return lastSignInAt;}
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getResetTokenHash() { return resetTokenHash; }
    public LocalDateTime getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void issueReset(String hash, LocalDateTime expiresAt) { resetTokenHash = hash; resetTokenExpiresAt = expiresAt; }
    public void changePassword(String hash) { passwordHash = hash; passwordChangedAt = LocalDateTime.now(); resetTokenHash = null; resetTokenExpiresAt = null; }
    public void recordSignIn(){lastSignInAt=LocalDateTime.now();}
    public void setActive(boolean value){active=value;}
    public void repairProfile(String name,String emailAddress){if(displayName==null||displayName.isBlank())displayName=name;if(email==null||email.isBlank())email=emailAddress;if(username==null||username.isBlank())username=emailAddress;}
}
