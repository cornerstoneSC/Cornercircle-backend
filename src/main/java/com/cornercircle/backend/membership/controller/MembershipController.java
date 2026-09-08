package com.cornercircle.backend.membership.controller;

import com.cornercircle.backend.membership.dto.*;
import com.cornercircle.backend.membership.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/membership-applications")
public class MembershipController {
    private final MembershipService service;
    public MembershipController(MembershipService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipApplicationResponse apply(@Valid @RequestBody MembershipApplicationRequest request) { return service.apply(request); }

    @PostMapping("/{applicationId}/checkout-session")
    public CheckoutSessionResponse checkout(@PathVariable UUID applicationId) { return service.checkout(applicationId); }

    @PostMapping("/{applicationId}/renewal-checkout-session")
    public CheckoutSessionResponse renewalCheckout(@PathVariable UUID applicationId) { return service.renewalCheckout(applicationId); }

    @GetMapping("/{applicationId}/status")
    public MembershipStatusResponse status(@PathVariable UUID applicationId) { return service.status(applicationId); }

    @PostMapping("/{applicationId}/billing-portal")
    public BillingPortalResponse billingPortal(@PathVariable UUID applicationId) { return service.billingPortal(applicationId); }
}
