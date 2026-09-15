package com.cornercircle.backend.serviceconsultation;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/service-consultations")
public class ServiceConsultationController {
    private final ServiceConsultationService service; private final String adminToken;
    public ServiceConsultationController(ServiceConsultationService service,@Value("${SERVICES_ADMIN_TOKEN:}") String adminToken){this.service=service;this.adminToken=adminToken;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ConsultationResponse create(@Valid @RequestBody ConsultationCreateRequest request){return service.create(request);}
    @GetMapping("/admin") public List<ConsultationResponse> list(@RequestHeader(value="Authorization",required=false) String authorization){authorize(authorization);return service.list();}
    @PatchMapping("/admin/{id}") public ConsultationResponse update(@RequestHeader(value="Authorization",required=false) String authorization,@PathVariable UUID id,@Valid @RequestBody ConsultationUpdateRequest request){authorize(authorization);return service.update(id,request);}
    private void authorize(String supplied){
        if(adminToken.length()<32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Services administration is not configured.");
        byte[] expected=("Bearer "+adminToken).getBytes(StandardCharsets.UTF_8), actual=supplied==null?new byte[0]:supplied.getBytes(StandardCharsets.UTF_8);
        if(!MessageDigest.isEqual(expected,actual)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid Services admin token.");
    }
}
