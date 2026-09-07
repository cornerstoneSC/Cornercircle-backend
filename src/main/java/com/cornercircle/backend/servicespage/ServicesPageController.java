package com.cornercircle.backend.servicespage;

import com.cornercircle.backend.media.dto.ImageUploadResponse;
import com.cornercircle.backend.media.service.CloudinaryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class ServicesPageController {
    private final ServicesPageRepository repository;
    private final ObjectMapper mapper;
    private final CloudinaryService media;
    private final String adminToken;
    public ServicesPageController(ServicesPageRepository repository, ObjectMapper mapper, CloudinaryService media,
        @Value("${SERVICES_ADMIN_TOKEN:}") String adminToken) {
        this.repository=repository; this.mapper=mapper; this.media=media; this.adminToken=adminToken;
    }
    void authorize(String authorization) {
        if (adminToken.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Services editing is not configured. Set SERVICES_ADMIN_TOKEN on the backend (at least 32 characters).");
        if (authorization == null || !MessageDigest.isEqual(("Bearer " + adminToken).getBytes(StandardCharsets.UTF_8), authorization.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Services admin token.");
    }
    @GetMapping("/api/v1/services-page")
    public ResponseEntity<JsonNode> get() throws Exception {
        var saved=repository.findById(1L);
        if (saved.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(mapper.readTree(saved.get().getContent()));
    }
    @PutMapping("/api/v1/admin/services-page")
    public JsonNode save(@RequestHeader(value="Authorization", required=false) String authorization, @RequestBody JsonNode content) {
        authorize(authorization);
        validate(content);
        var entity=repository.findById(1L).orElseGet(()->new ServicesPageEntity("{}"));
        entity.setContent(content.toString()); repository.save(entity); return content;
    }
    @PostMapping(value="/api/v1/admin/services-page/image", consumes="multipart/form-data")
    public ImageUploadResponse upload(@RequestHeader(value="Authorization", required=false) String authorization, @RequestParam("file") MultipartFile file) {
        authorize(authorization);
        return media.uploadImage(file,"cornerstone/services");
    }
    static void validate(JsonNode content) {
        if (!content.isObject() || content.toString().length()>60000) bad();
        for (String field : new String[]{"heroLabel","heroTitle","heroDescription","heroCta","howLabel","heroImage","heroAlt","gardenImage","gardenAlt","note","approachTitle","approachDescription","stepsTitle","faqTitle","enquiryTitle","enquiryDescription"}) text(content,field);
        for (String field : new String[]{"heroImage","gardenImage"}) {
            String url=content.path(field).asText();
            if (!url.matches("(/images/[^\\s]*|https://[^\\s]+)")) bad();
        }
        rows(content,"offerings",4,new String[]{"title","description"});
        rows(content,"steps",3,new String[]{"title","text"});
        rows(content,"questions",4,new String[]{"question","answer"});
        for (JsonNode question:content.path("questions")) {
            JsonNode items=question.path("items");
            if (!items.isArray() || items.size()>20) bad();
            for(JsonNode item:items) if(!item.isTextual() || item.asText().isBlank() || item.asText().length()>500) bad();
        }
    }
    private static void rows(JsonNode node,String field,int count,String[] fields) {
        JsonNode rows=node.path(field);
        if(!rows.isArray() || rows.size()!=count) bad();
        for(JsonNode row:rows) for(String key:fields) text(row,key);
    }
    private static void text(JsonNode node,String field) {
        JsonNode value=node.path(field);
        if(!value.isTextual() || value.asText().isBlank() || value.asText().length()>3000) bad();
    }
    private static void bad() { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Services content. Complete all fields and keep four offerings, three steps and four FAQs."); }
}
