package com.cornercircle.backend.homepage.controller;

import com.cornercircle.backend.homepage.dto.HomepageResponse;
import com.cornercircle.backend.homepage.dto.HomepageContentRequest;
import com.cornercircle.backend.homepage.service.HomepageService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/admin/homepage")
public class HomepageController {
    private final HomepageService homepageService;
    @Value("${admin.content-api-secret:}")
    private String contentApiSecret;

    public HomepageController(HomepageService homepageService) {
        this.homepageService = homepageService;
    }
    @PostMapping(value = "/hero/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomepageResponse> uploadHeroImage(
            @RequestHeader(value = "X-Admin-Content-Secret", required = false) String suppliedSecret,
            @RequestParam("file") MultipartFile file) {
        authorize(suppliedSecret);
        return ResponseEntity.ok(homepageService.uploadHeroImage(file));
    }

    @PostMapping(value = "/about/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomepageResponse> uploadAboutImage(
            @RequestHeader(value = "X-Admin-Content-Secret", required = false) String suppliedSecret,
            @RequestParam("file") MultipartFile file) {
        authorize(suppliedSecret);
        return ResponseEntity.ok(homepageService.uploadAboutImage(file));
    }

    @PostMapping(value = "/beliefs/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomepageResponse> uploadBeliefsImage(
            @RequestHeader(value = "X-Admin-Content-Secret", required = false) String suppliedSecret,
            @RequestParam("file") MultipartFile file) {
        authorize(suppliedSecret);
        return ResponseEntity.ok(homepageService.uploadBeliefsImage(file));
    }

    @PostMapping(value = "/founder/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomepageResponse> uploadFounderImage(
            @RequestHeader(value = "X-Admin-Content-Secret", required = false) String suppliedSecret,
            @RequestParam("file") MultipartFile file) {
        authorize(suppliedSecret);
        return ResponseEntity.ok(homepageService.uploadFounderImage(file));
    }

    @GetMapping
    public ResponseEntity<HomepageResponse> getHomepage(){
        HomepageResponse homepage = homepageService.getHomepage();
        return ResponseEntity.ok(homepage);
    }

    @PutMapping("/content")
    public ResponseEntity<HomepageResponse> updateContent(
            @RequestHeader(value = "X-Admin-Content-Secret", required = false) String suppliedSecret,
            @RequestBody HomepageContentRequest request) {
        authorize(suppliedSecret);
        if (request.contentJson() == null || request.contentJson().length() > 100_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid homepage content");
        }
        return ResponseEntity.ok(homepageService.updateContent(request));
    }

    private void authorize(String suppliedSecret) {
        if (contentApiSecret.isBlank() || suppliedSecret == null ||
                !MessageDigest.isEqual(contentApiSecret.getBytes(StandardCharsets.UTF_8), suppliedSecret.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin authorization required");
    }
}
