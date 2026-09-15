package com.cornercircle.backend.adminauth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminCredentialRepository extends JpaRepository<AdminCredential, Long> {
    Optional<AdminCredential> findByUsernameIgnoreCase(String username);
    Optional<AdminCredential> findByEmailIgnoreCase(String email);
    Optional<AdminCredential> findByResetTokenHash(String hash);
    long countByActiveTrue();
}
