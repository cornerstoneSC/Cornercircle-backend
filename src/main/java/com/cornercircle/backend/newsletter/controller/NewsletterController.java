package com.cornercircle.backend.newsletter.controller;
import com.cornercircle.backend.newsletter.dto.*; import com.cornercircle.backend.newsletter.model.NewsletterStatus; import com.cornercircle.backend.newsletter.service.NewsletterService;
import org.springframework.beans.factory.annotation.Value; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.util.*;
@RestController @RequestMapping("/api/v1/newsletter") public class NewsletterController{
 private final NewsletterService service;private final String adminToken;public NewsletterController(NewsletterService service,@Value("${MEMBERSHIP_ADMIN_TOKEN:}")String adminToken){this.service=service;this.adminToken=adminToken;}
 @PostMapping("/subscribe")public ResponseEntity<Void> subscribe(@RequestBody NewsletterSubscribeRequest request){if(request.website()==null||request.website().isBlank())service.subscribe(request.email());return ResponseEntity.noContent().build();}
 @PostMapping("/unsubscribe/{token}")public ResponseEntity<Void> unsubscribe(@PathVariable UUID token){service.unsubscribe(token);return ResponseEntity.noContent().build();}
 @GetMapping("/admin/subscribers")public List<NewsletterSubscriberResponse> list(@RequestHeader(value="Authorization",required=false)String authorization,@RequestParam(defaultValue="")String query,@RequestParam(required=false)NewsletterStatus status){authorize(authorization);return service.list(query,status);}
 private void authorize(String authorization){if(adminToken.length()<32)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Newsletter administration is not configured.");byte[]expected=("Bearer "+adminToken).getBytes(StandardCharsets.UTF_8);byte[]supplied=authorization==null?new byte[0]:authorization.getBytes(StandardCharsets.UTF_8);if(!MessageDigest.isEqual(expected,supplied))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid admin token.");}
}
