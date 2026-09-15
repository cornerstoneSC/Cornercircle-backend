package com.cornercircle.backend.adminauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    private final AdminAuthService service; private final String serverToken;
    public AdminAuthController(AdminAuthService service,@Value("${MEMBERSHIP_ADMIN_TOKEN:}")String serverToken){this.service=service;this.serverToken=serverToken;}
    @PostMapping("/login") public ResponseEntity<Void> login(@RequestHeader(value="X-Admin-Auth-Token",required=false)String token,@RequestBody Login request){authorize(token);return switch(service.authenticate(request.username(),request.password())){case AUTHENTICATED->ResponseEntity.noContent().build();case REJECTED->ResponseEntity.status(401).build();case NOT_CONFIGURED->ResponseEntity.status(503).build();};}
    @PostMapping("/forgot-password") public ResponseEntity<Void> forgot(@RequestHeader(value="X-Admin-Auth-Token",required=false)String token,@RequestBody Forgot request){authorize(token);service.requestReset(request.email());return ResponseEntity.noContent().build();}
    @PostMapping("/reset-password") public ResponseEntity<Void> reset(@RequestHeader(value="X-Admin-Auth-Token",required=false)String token,@RequestBody Reset request){authorize(token);return service.resetPassword(request.token(),request.password())?ResponseEntity.noContent().build():ResponseEntity.badRequest().build();}
    @GetMapping("/administrators") public java.util.List<AdminAuthService.AdminSummary> list(@RequestHeader(value="X-Admin-Auth-Token",required=false)String token){authorize(token);return service.list();}
    @PostMapping("/administrators") public AdminAuthService.AdminSummary add(@RequestHeader(value="X-Admin-Auth-Token",required=false)String token,@RequestBody Add request){authorize(token);return service.add(request.displayName(),request.email());}
    @PostMapping("/administrators/{id}/reset-password") public ResponseEntity<Void> resetFor(@RequestHeader(value="X-Admin-Auth-Token",required=false)String token,@PathVariable Long id){authorize(token);service.resetFor(id);return ResponseEntity.noContent().build();}
    @PostMapping("/administrators/{id}/active") public AdminAuthService.AdminSummary active(@RequestHeader(value="X-Admin-Auth-Token",required=false)String token,@PathVariable Long id,@RequestBody Active request){authorize(token);return service.setActive(id,request.active());}
    private void authorize(String supplied){byte[]a=serverToken.getBytes(StandardCharsets.UTF_8),b=supplied==null?new byte[0]:supplied.getBytes(StandardCharsets.UTF_8);if(serverToken.length()<32||!MessageDigest.isEqual(a,b))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);}
    public record Login(String username,String password){} public record Forgot(String email){} public record Reset(String token,String password){} public record Add(String displayName,String email){} public record Active(boolean active){}
}
