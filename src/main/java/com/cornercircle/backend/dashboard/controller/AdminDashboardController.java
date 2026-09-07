package com.cornercircle.backend.dashboard.controller;
import com.cornercircle.backend.dashboard.dto.AdminDashboardResponse;
import com.cornercircle.backend.dashboard.service.AdminDashboardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@RestController @RequestMapping("/api/v1/admin/dashboard") public class AdminDashboardController {
 private final AdminDashboardService service; private final String adminToken;
 public AdminDashboardController(AdminDashboardService service,@Value("${MEMBERSHIP_ADMIN_TOKEN:}")String adminToken){this.service=service;this.adminToken=adminToken;}
 @GetMapping public AdminDashboardResponse get(@RequestHeader(value="Authorization",required=false)String authorization){authorize(authorization);return service.get();}
 private void authorize(String authorization){if(adminToken.length()<32)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Dashboard administration is not configured.");byte[]expected=("Bearer "+adminToken).getBytes(StandardCharsets.UTF_8);byte[]supplied=authorization==null?new byte[0]:authorization.getBytes(StandardCharsets.UTF_8);if(!MessageDigest.isEqual(expected,supplied))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid admin token.");}
}
