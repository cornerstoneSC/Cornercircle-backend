package com.cornercircle.backend.contactpage;

import com.cornercircle.backend.media.dto.ImageUploadResponse;
import com.cornercircle.backend.media.service.CloudinaryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class ContactPageController {
    private final ContactPageRepository repository; private final ObjectMapper mapper; private final CloudinaryService media; private final String adminToken;
    public ContactPageController(ContactPageRepository repository, ObjectMapper mapper, CloudinaryService media, @Value("${SERVICES_ADMIN_TOKEN:}") String adminToken) { this.repository=repository; this.mapper=mapper; this.media=media; this.adminToken=adminToken; }
    private void authorize(String authorization) {
        if (adminToken.length()<32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Contact editing is not configured.");
        if (authorization==null || !MessageDigest.isEqual(("Bearer "+adminToken).getBytes(StandardCharsets.UTF_8),authorization.getBytes(StandardCharsets.UTF_8))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid Contact admin token.");
    }
    @GetMapping("/api/v1/contact-page")
    public ResponseEntity<JsonNode> get() throws Exception { var saved=repository.findById(1L); return saved.isEmpty()?ResponseEntity.noContent().build():ResponseEntity.ok(mapper.readTree(saved.get().getContent())); }
    @PutMapping("/api/v1/admin/contact-page")
    public JsonNode save(@RequestHeader(value="Authorization",required=false) String authorization,@RequestBody JsonNode content) {
        authorize(authorization); validate(content); var entity=repository.findById(1L).orElseGet(()->new ContactPageEntity("{}")); entity.setContent(content.toString()); repository.save(entity); return content;
    }
    @PostMapping(value="/api/v1/admin/contact-page/image",consumes="multipart/form-data")
    public ImageUploadResponse upload(@RequestHeader(value="Authorization",required=false) String authorization,@RequestParam("file") MultipartFile file) { authorize(authorization); return media.uploadImage(file,"cornerstone/contact"); }
    private static void validate(JsonNode content) {
        if (!content.isObject() || content.toString().length()>20000) bad();
        for (String field:new String[]{"eyebrow","title","accentTitle","description","imageUrl","imageAlt","imageNote","formLabel"}) { JsonNode value=content.path(field); if(!value.isTextual()||value.asText().isBlank()||value.asText().length()>3000) bad(); }
        if(!content.path("imageUrl").asText().matches("(/images/[^\\s]*|https://[^\\s]+)")) bad();
    }
    private static void bad(){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete all Contact page fields."); }
}
