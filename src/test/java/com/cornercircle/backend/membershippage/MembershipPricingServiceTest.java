package com.cornercircle.backend.membershippage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MembershipPricingServiceTest {
    private final MembershipPageRepository repository = mock(MembershipPageRepository.class);
    private final MembershipPricingService service = new MembershipPricingService(repository, new ObjectMapper());

    @Test
    void usesAdminConfiguredAnnualPrice() {
        when(repository.findById(1L)).thenReturn(Optional.of(new MembershipPageEntity("{\"annualPriceCents\":24900}")));
        assertEquals(24_900L, service.annualPriceCents());
    }

    @Test
    void safelyDefaultsForLegacyContent() {
        when(repository.findById(1L)).thenReturn(Optional.of(new MembershipPageEntity("{\"price\":\"$199\"}")));
        assertEquals(19_900L, service.annualPriceCents());
    }
}
