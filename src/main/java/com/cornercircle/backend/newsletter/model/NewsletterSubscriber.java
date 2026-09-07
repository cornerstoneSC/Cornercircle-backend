package com.cornercircle.backend.newsletter.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Table(name="newsletter_subscribers", uniqueConstraints=@UniqueConstraint(name="uk_newsletter_email", columnNames="email"))
public class NewsletterSubscriber {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=320) private String email;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private NewsletterStatus status;
 @Column(name="unsubscribe_token",nullable=false,unique=true) private UUID unsubscribeToken;
 @Column(name="subscribed_at",nullable=false) private LocalDateTime subscribedAt;
 @Column(name="unsubscribed_at") private LocalDateTime unsubscribedAt;
 protected NewsletterSubscriber() {}
 public NewsletterSubscriber(String email){this.email=email;status=NewsletterStatus.ACTIVE;unsubscribeToken=UUID.randomUUID();subscribedAt=LocalDateTime.now();}
 public void reactivate(){status=NewsletterStatus.ACTIVE;subscribedAt=LocalDateTime.now();unsubscribedAt=null;unsubscribeToken=UUID.randomUUID();}
 public void unsubscribe(){status=NewsletterStatus.UNSUBSCRIBED;unsubscribedAt=LocalDateTime.now();}
 public Long getId(){return id;} public String getEmail(){return email;} public NewsletterStatus getStatus(){return status;} public UUID getUnsubscribeToken(){return unsubscribeToken;} public LocalDateTime getSubscribedAt(){return subscribedAt;} public LocalDateTime getUnsubscribedAt(){return unsubscribedAt;}
}
