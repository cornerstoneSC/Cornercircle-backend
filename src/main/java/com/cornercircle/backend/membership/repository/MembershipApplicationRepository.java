package com.cornercircle.backend.membership.repository;

import com.cornercircle.backend.membership.model.MembershipApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import com.cornercircle.backend.membership.model.MembershipStatus;

public interface MembershipApplicationRepository extends JpaRepository<MembershipApplication, Long> {
    Optional<MembershipApplication> findByPublicId(UUID publicId);
    Optional<MembershipApplication> findByStripeCustomerId(String customerId);
    Optional<MembershipApplication> findByStripeSubscriptionId(String subscriptionId);
    Optional<MembershipApplication> findByStripePaymentIntentId(String paymentIntentId);
    Optional<MembershipApplication> findFirstByEmailAndStatusInOrderByCreatedAtDesc(String email, Collection<MembershipStatus> statuses);
}
