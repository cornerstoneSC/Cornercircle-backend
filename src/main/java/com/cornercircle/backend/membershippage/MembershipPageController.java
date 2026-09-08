package com.cornercircle.backend.membershippage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class MembershipPageController {
    private final MembershipPageRepository repository;
    private final ObjectMapper mapper;
    private final String adminToken;

    public MembershipPageController(MembershipPageRepository repository, ObjectMapper mapper,
        @Value("${MEMBERSHIP_ADMIN_TOKEN:}") String adminToken) {
        this.repository = repository;
        this.mapper = mapper;
        this.adminToken = adminToken;
    }

    void authorize(String authorization) {
        if (adminToken.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Membership editing is not configured. Set MEMBERSHIP_ADMIN_TOKEN on the backend (at least 32 characters).");
        if (authorization == null || !MessageDigest.isEqual(("Bearer " + adminToken).getBytes(StandardCharsets.UTF_8), authorization.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Membership admin token.");
    }

    @GetMapping("/api/v1/membership-page")
    public ResponseEntity<JsonNode> get() throws Exception {
        var saved = repository.findById(1L);
        if (saved.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(mapper.readTree(saved.get().getContent()));
    }

    @PutMapping("/api/v1/admin/membership-page")
    public JsonNode save(@RequestHeader(value="Authorization", required=false) String authorization, @RequestBody JsonNode content) {
        authorize(authorization);
        validate(content);
        long cents = content.path("annualPriceCents").asLong();
        ((com.fasterxml.jackson.databind.node.ObjectNode) content).put("price", formatPrice(cents));
        var entity = repository.findById(1L).orElseGet(() -> new MembershipPageEntity("{}"));
        entity.setContent(content.toString());
        repository.save(entity);
        return content;
    }

    static void validate(JsonNode content) {
        if (!content.isObject() || content.toString().length() > 60000) bad();
        if (!content.path("annualPriceCents").canConvertToLong()) badPrice();
        long cents = content.path("annualPriceCents").asLong();
        if (cents < 100 || cents > 1_000_000) badPrice();
        for (String field : new String[]{"eyebrow","title","price","pricePeriod","tagline","stepOneTitle","stepOneIntro","stepTwoTitle","stepTwoIntro","stepThreeTitle","stepThreeIntro","activitiesLegend","goalsLegend","agreementTitle","agreementText","photographyTitle","photographyAcknowledgement","commentsLabel"}) text(content, field);
        list(content, "benefits", 6);
        list(content, "stepLabels", 3);
        list(content, "activities", 8);
        list(content, "goals", 10);
        list(content, "photographyParagraphs", 2);
    }

    private static void list(JsonNode node, String field, int count) {
        JsonNode values = node.path(field);
        if (!values.isArray() || values.size() != count) bad();
        for (JsonNode value : values) if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > 3000) bad();
    }

    private static void text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > 3000) bad();
    }

    private static void bad() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Membership content. Complete all editable fields and keep the existing list lengths.");
    }

    private static void badPrice() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Annual membership price must be between $1.00 and $10,000.00.");
    }

    private static String formatPrice(long cents) {
        java.math.BigDecimal dollars = java.math.BigDecimal.valueOf(cents, 2).stripTrailingZeros();
        return "$" + dollars.toPlainString();
    }
}
