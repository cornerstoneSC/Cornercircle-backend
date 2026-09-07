package com.cornercircle.backend.newsletter.repository;
import com.cornercircle.backend.newsletter.model.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; import java.util.UUID;
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber,Long>{Optional<NewsletterSubscriber> findByEmailIgnoreCase(String email);Optional<NewsletterSubscriber> findByUnsubscribeToken(UUID token);}
