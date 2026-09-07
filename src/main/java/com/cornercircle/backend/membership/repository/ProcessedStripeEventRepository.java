package com.cornercircle.backend.membership.repository;

import com.cornercircle.backend.membership.model.ProcessedStripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedStripeEventRepository extends JpaRepository<ProcessedStripeEvent, String> {}
