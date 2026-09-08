package com.cornercircle.backend.membership.controller;

import com.cornercircle.backend.membership.dto.MembershipApplicationRequest;
import com.cornercircle.backend.membership.service.MembershipService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MembershipValidationExceptionHandlerTest {
    @Test
    void returnsFieldSpecificErrorsForInvalidApplication() throws Exception {
        MembershipService service = mock(MembershipService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MembershipController(service))
            .setControllerAdvice(new MembershipValidationExceptionHandler())
            .build();

        mvc.perform(post("/api/v1/membership-applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"","email":"invalid","phone":"","city":"","birthday":"2999-01-01",
                     "inspiredBy":"","activities":[],"goals":[],"membershipAgreementAccepted":false,
                     "photographyNoticeAcknowledged":false,"comments":""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Please correct the highlighted membership fields."))
            .andExpect(jsonPath("$.fieldErrors.fullName").exists())
            .andExpect(jsonPath("$.fieldErrors.email").exists())
            .andExpect(jsonPath("$.fieldErrors.city").exists())
            .andExpect(jsonPath("$.fieldErrors.birthday").exists())
            .andExpect(jsonPath("$.fieldErrors.activities").exists())
            .andExpect(jsonPath("$.fieldErrors.goals").exists())
            .andExpect(jsonPath("$.fieldErrors.membershipAgreementAccepted").exists())
            .andExpect(jsonPath("$.fieldErrors.photographyNoticeAcknowledged").exists());
    }

    @Test
    void preservesSafeMembershipConflictMessage() throws Exception {
        MembershipService service = mock(MembershipService.class);
        java.util.UUID id = java.util.UUID.randomUUID();
        when(service.checkout(id)).thenThrow(new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT, "This membership is already active."));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MembershipController(service))
            .setControllerAdvice(new MembershipValidationExceptionHandler())
            .build();

        mvc.perform(post("/api/v1/membership-applications/{id}/checkout-session", id))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("This membership is already active."));
    }
}
