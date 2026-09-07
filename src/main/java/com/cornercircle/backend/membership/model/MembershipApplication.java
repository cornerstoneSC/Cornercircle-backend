package com.cornercircle.backend.membership.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "membership_applications", indexes = {
    @Index(name = "idx_membership_stripe_customer", columnList = "stripe_customer_id"),
    @Index(name = "idx_membership_stripe_subscription", columnList = "stripe_subscription_id")
})
public class MembershipApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false) private UUID publicId;
    @Column(nullable = false, length = 160) private String fullName;
    @Column(nullable = false, length = 254) private String email;
    @Column(length = 40) private String phone;
    @Column(nullable = false, length = 120) private String city;
    private LocalDate birthday;
    @Column(name = "inspired_by", nullable = false, columnDefinition = "TEXT") private String inspiredBy;
    @ElementCollection @CollectionTable(name = "membership_application_activities", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "activity", nullable = false, length = 200) private List<String> activities = new ArrayList<>();
    @ElementCollection @CollectionTable(name = "membership_application_goals", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "goal", nullable = false, length = 200) private List<String> goals = new ArrayList<>();
    @Column(name = "membership_agreement_accepted", nullable = false) private boolean membershipAgreementAccepted;
    @Column(name = "photography_notice_acknowledged", nullable = false) private boolean photographyNoticeAcknowledged;
    @Column(columnDefinition = "TEXT") private String comments;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private MembershipStatus status;
    @Column(name = "stripe_checkout_session_id", unique = true, length = 255) private String stripeCheckoutSessionId;
    @Column(name = "stripe_customer_id", length = 255) private String stripeCustomerId;
    @Column(name = "stripe_subscription_id", unique = true, length = 255) private String stripeSubscriptionId;
    @Column(name = "stripe_payment_intent_id", unique = true, length = 255) private String stripePaymentIntentId;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "membership_starts_on") private LocalDate membershipStartsOn;
    @Column(name = "membership_ends_on") private LocalDate membershipEndsOn;
    @Column(name = "renewal_reminder_sent_at") private LocalDateTime renewalReminderSentAt;
    @Column(name = "internal_notes", columnDefinition = "TEXT") private String internalNotes;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void create() { var now = LocalDateTime.now(); if (publicId == null) publicId = UUID.randomUUID(); if (status == null) status = MembershipStatus.PENDING_PAYMENT; createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public void setPublicId(UUID publicId) { this.publicId = publicId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public String getInspiredBy() { return inspiredBy; }
    public void setInspiredBy(String inspiredBy) { this.inspiredBy = inspiredBy; }
    public List<String> getActivities() { return activities; }
    public void setActivities(List<String> activities) { this.activities = new ArrayList<>(activities); }
    public List<String> getGoals() { return goals; }
    public void setGoals(List<String> goals) { this.goals = new ArrayList<>(goals); }
    public boolean isMembershipAgreementAccepted() { return membershipAgreementAccepted; }
    public void setMembershipAgreementAccepted(boolean accepted) { this.membershipAgreementAccepted = accepted; }
    public boolean isPhotographyNoticeAcknowledged() { return photographyNoticeAcknowledged; }
    public void setPhotographyNoticeAcknowledged(boolean acknowledged) { this.photographyNoticeAcknowledged = acknowledged; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public MembershipStatus getStatus() { return status; }
    public void setStatus(MembershipStatus status) { this.status = status; }
    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public void setStripeCheckoutSessionId(String value) { this.stripeCheckoutSessionId = value; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String value) { this.stripeCustomerId = value; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String value) { this.stripeSubscriptionId = value; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String value) { this.stripePaymentIntentId = value; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDate getMembershipStartsOn() { return membershipStartsOn; }
    public void setMembershipStartsOn(LocalDate membershipStartsOn) { this.membershipStartsOn = membershipStartsOn; }
    public LocalDate getMembershipEndsOn() { return membershipEndsOn; }
    public void setMembershipEndsOn(LocalDate membershipEndsOn) { this.membershipEndsOn = membershipEndsOn; }
    public LocalDateTime getRenewalReminderSentAt() { return renewalReminderSentAt; }
    public void setRenewalReminderSentAt(LocalDateTime renewalReminderSentAt) { this.renewalReminderSentAt = renewalReminderSentAt; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
