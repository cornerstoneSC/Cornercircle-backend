package com.cornercircle.backend.membership.controller;

import com.cornercircle.backend.membership.dto.*;
import com.cornercircle.backend.membership.service.MembersAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/members")
public class MembersAdminController {
    private final MembersAdminService service;
    private final String adminToken;

    public MembersAdminController(MembersAdminService service,
        @Value("${MEMBERSHIP_ADMIN_TOKEN:}") String adminToken) {
        this.service = service;
        this.adminToken = adminToken;
    }

    @GetMapping
    public AdminMembersResponse list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestParam(defaultValue = "") String query) {
        authorize(authorization);
        return service.list(query);
    }

    @GetMapping("/{publicId}")
    public AdminMemberResponse get(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable UUID publicId) {
        authorize(authorization);
        return service.get(publicId);
    }

    @PostMapping("/{publicId}/renewal-reminder")
    public AdminMemberResponse recordReminder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                              @PathVariable UUID publicId) {
        authorize(authorization);
        return service.recordReminder(publicId);
    }

    @PatchMapping("/{publicId}/notes")
    public AdminMemberResponse updateNotes(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable UUID publicId,
                                           @RequestBody MemberNotesRequest request) {
        authorize(authorization);
        return service.updateNotes(publicId, request);
    }

    @PostMapping("/{publicId}/welcome-email")
    public AdminMemberResponse sendWelcomeEmail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable UUID publicId) {
        authorize(authorization);
        return service.sendWelcomeEmail(publicId);
    }

    private void authorize(String authorization) {
        if (adminToken.length() < 32)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Members administration is not configured. Set MEMBERSHIP_ADMIN_TOKEN on the backend (at least 32 characters).");
        byte[] expected = ("Bearer " + adminToken).getBytes(StandardCharsets.UTF_8);
        byte[] supplied = authorization == null ? new byte[0] : authorization.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, supplied))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Membership admin token.");
    }
}
