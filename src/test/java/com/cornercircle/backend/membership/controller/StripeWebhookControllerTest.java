package com.cornercircle.backend.membership.controller;

import com.cornercircle.backend.membership.service.StripeWebhookService;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripeWebhookControllerTest {
    private static final String SECRET = "whsec_test_secret_that_is_long_enough";
    private final StripeWebhookService service = mock(StripeWebhookService.class);
    private final StripeWebhookController controller = new StripeWebhookController(service, SECRET);

    @Test void acceptsValidStripeSignature() throws Exception {
        String payload = "{\"id\":\"evt_123\",\"object\":\"event\",\"type\":\"invoice.paid\",\"data\":{\"object\":{}}}";
        String signature = Webhook.Signature.generateSignatureHeader(payload, SECRET);
        var response = controller.webhook(payload, signature);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).process("evt_123", "invoice.paid", payload);
    }

    @Test void rejectsInvalidStripeSignature() {
        var response = controller.webhook("{}", "t=1,v1=invalid");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(service);
    }
}
