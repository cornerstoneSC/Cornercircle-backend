package com.cornercircle.backend.eventpolicy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class EventPolicyController {
    private final EventPolicyRepository repository;
    private final ObjectMapper mapper;
    private final String adminToken;

    public EventPolicyController(EventPolicyRepository repository, ObjectMapper mapper,
            @Value("${MEMBERSHIP_ADMIN_TOKEN:}") String adminToken) {
        this.repository = repository;
        this.mapper = mapper;
        this.adminToken = adminToken;
    }

    @GetMapping("/api/v1/event-policy")
    public ResponseEntity<JsonNode> get() throws Exception {
        var saved = repository.findById(1L);
        if (saved.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(mapper.readTree(saved.get().getContent()));
    }

    @PutMapping("/api/v1/admin/event-policy")
    public JsonNode save(@RequestHeader(value="Authorization", required=false) String authorization,
            @RequestBody JsonNode content) {
        authorize(authorization);
        validate(content);
        var entity = repository.findById(1L).orElseGet(() -> new EventPolicyEntity("{}"));
        entity.setContent(content.toString());
        repository.save(entity);
        return content;
    }

    private void authorize(String authorization) {
        if (adminToken.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Event policy editing is not configured.");
        if (authorization == null || !MessageDigest.isEqual(("Bearer " + adminToken).getBytes(StandardCharsets.UTF_8), authorization.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin token.");
    }

    static void validate(JsonNode content) {
        if (!content.isObject() || content.toString().length() > 60000) bad();
        if (!content.path("effectiveDate").isTextual() || content.path("effectiveDate").asText().isBlank()) bad();
        if (!content.path("introduction").isTextual() || content.path("introduction").asText().isBlank()) bad();
        var sections = content.path("sections");
        if (!sections.isArray() || sections.size() != 17) bad();
        for (var section : sections) {
            if (!section.path("title").isTextual() || section.path("title").asText().isBlank()) bad();
            var body = section.path("body");
            if (!body.isArray() || body.isEmpty()) bad();
            for (var paragraph : body) if (!paragraph.isTextual() || paragraph.asText().isBlank() || paragraph.asText().length() > 5000) bad();
        }
    }

    private static void bad() { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Event Policy content."); }
}
