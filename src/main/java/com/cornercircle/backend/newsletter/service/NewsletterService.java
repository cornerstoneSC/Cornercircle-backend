package com.cornercircle.backend.newsletter.service;
import com.cornercircle.backend.newsletter.dto.NewsletterSubscriberResponse; import com.cornercircle.backend.newsletter.model.*; import com.cornercircle.backend.newsletter.repository.NewsletterSubscriberRepository;
import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException;
import java.util.*; import java.util.regex.Pattern;
@Service public class NewsletterService{
 private static final Pattern EMAIL=Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"); private final NewsletterSubscriberRepository repository;
 public NewsletterService(NewsletterSubscriberRepository repository){this.repository=repository;}
 @Transactional public void subscribe(String raw){String email=raw==null?"":raw.trim().toLowerCase(Locale.ROOT);if(email.length()>320||!EMAIL.matcher(email).matches())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid email address.");var existing=repository.findByEmailIgnoreCase(email);if(existing.isPresent()){if(existing.get().getStatus()==NewsletterStatus.UNSUBSCRIBED)existing.get().reactivate();return;}repository.save(new NewsletterSubscriber(email));}
 @Transactional public void unsubscribe(UUID token){NewsletterSubscriber subscriber=repository.findByUnsubscribeToken(token).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"This unsubscribe link is invalid."));subscriber.unsubscribe();}
 @Transactional(readOnly=true) public List<NewsletterSubscriberResponse> list(String query,NewsletterStatus status){String needle=query==null?"":query.trim().toLowerCase(Locale.ROOT);return repository.findAll().stream().filter(s->status==null||s.getStatus()==status).filter(s->needle.isBlank()||s.getEmail().contains(needle)).sorted((a,b)->b.getSubscribedAt().compareTo(a.getSubscribedAt())).map(s->new NewsletterSubscriberResponse(s.getId(),s.getEmail(),s.getStatus(),s.getSubscribedAt(),s.getUnsubscribedAt(),s.getUnsubscribeToken())).toList();}
}
