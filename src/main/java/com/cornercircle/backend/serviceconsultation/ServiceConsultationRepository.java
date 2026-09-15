package com.cornercircle.backend.serviceconsultation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceConsultationRepository extends JpaRepository<ServiceConsultation, UUID> {
    List<ServiceConsultation> findAllByOrderByCreatedAtDesc();
}
