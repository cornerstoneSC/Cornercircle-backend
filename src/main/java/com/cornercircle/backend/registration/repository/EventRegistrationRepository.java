package com.cornercircle.backend.registration.repository;
import com.cornercircle.backend.registration.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.time.LocalDateTime;
public interface EventRegistrationRepository extends JpaRepository<EventRegistration,Long>{
 Optional<EventRegistration> findByPublicId(UUID publicId);
 Optional<EventRegistration> findByStripePaymentIntentId(String stripePaymentIntentId);
 long countByEventId(Long eventId);
 @Query("select coalesce(sum(r.guestCount),0) from EventRegistration r where r.event.id=:eventId and r.status in :statuses") long reservedSeats(@Param("eventId")Long eventId,@Param("statuses")Collection<EventRegistrationStatus> statuses);
 @Query("select coalesce(sum(r.guestCount),0) from EventRegistration r where r.event.id=:eventId and (r.status=com.cornercircle.backend.registration.model.EventRegistrationStatus.CONFIRMED or (r.status=com.cornercircle.backend.registration.model.EventRegistrationStatus.PENDING_PAYMENT and r.createdAt>=:cutoff))") long activeReservedSeats(@Param("eventId")Long eventId,@Param("cutoff")LocalDateTime cutoff);
}
