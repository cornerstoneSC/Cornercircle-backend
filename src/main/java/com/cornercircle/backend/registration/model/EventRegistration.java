package com.cornercircle.backend.registration.model;
import com.cornercircle.backend.events.model.Event;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Table(name="event_registrations")
public class EventRegistration {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,updatable=false) private UUID publicId;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) private Event event;
 @Column(nullable=false,length=120) private String fullName;
 @Column(nullable=false,length=200) private String email;
 @Column(length=40) private String phone;
 @Column(nullable=false) private Integer guestCount;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal totalAmount;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private EventRegistrationStatus status;
 @Column(unique=true,length=255) private String stripeCheckoutSessionId;
 @Column(unique=true,length=255) private String stripePaymentIntentId;
 @Column(length=30) private String acceptedPolicyVersion;
 private LocalDateTime policyAcceptedAt;
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
 private LocalDateTime confirmedAt;
 protected EventRegistration() {}
 public EventRegistration(Event event,String fullName,String email,String phone,int guestCount,BigDecimal totalAmount,EventRegistrationStatus status){this.publicId=UUID.randomUUID();this.event=event;this.fullName=fullName.trim();this.email=email.trim().toLowerCase();this.phone=phone==null?"":phone.trim();this.guestCount=guestCount;this.totalAmount=totalAmount;setStatus(status);}
 @PrePersist void created(){if(publicId==null)publicId=UUID.randomUUID();createdAt=LocalDateTime.now();}
 public UUID getPublicId(){return publicId;} public Event getEvent(){return event;} public String getFullName(){return fullName;} public String getEmail(){return email;} public String getPhone(){return phone;} public Integer getGuestCount(){return guestCount;} public BigDecimal getTotalAmount(){return totalAmount;} public EventRegistrationStatus getStatus(){return status;} public String getStripeCheckoutSessionId(){return stripeCheckoutSessionId;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getConfirmedAt(){return confirmedAt;}
 public String getAcceptedPolicyVersion(){return acceptedPolicyVersion;} public LocalDateTime getPolicyAcceptedAt(){return policyAcceptedAt;}
 public void recordPolicyAcceptance(String version){acceptedPolicyVersion=version;policyAcceptedAt=LocalDateTime.now();}
 public void setStripeCheckoutSessionId(String value){stripeCheckoutSessionId=value;} public void setStatus(EventRegistrationStatus value){status=value;if(value==EventRegistrationStatus.CONFIRMED&&confirmedAt==null)confirmedAt=LocalDateTime.now();}
 public String getStripePaymentIntentId(){return stripePaymentIntentId;} public void setStripePaymentIntentId(String value){stripePaymentIntentId=value;}
}
