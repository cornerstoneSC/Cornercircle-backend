package com.cornercircle.backend.membershippage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class MembershipPricingService {
    public static final long DEFAULT_ANNUAL_PRICE_CENTS = 19_900L;
    private final MembershipPageRepository repository;
    private final ObjectMapper mapper;

    public MembershipPricingService(MembershipPageRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public long annualPriceCents() {
        return repository.findById(1L).map(entity -> {
            try {
                long value = mapper.readTree(entity.getContent()).path("annualPriceCents").asLong(DEFAULT_ANNUAL_PRICE_CENTS);
                return value >= 100 && value <= 1_000_000 ? value : DEFAULT_ANNUAL_PRICE_CENTS;
            } catch (Exception ignored) {
                return DEFAULT_ANNUAL_PRICE_CENTS;
            }
        }).orElse(DEFAULT_ANNUAL_PRICE_CENTS);
    }
}
