package com.cornercircle.backend.dashboard.service;
import com.cornercircle.backend.dashboard.dto.AdminDashboardResponse;
import com.cornercircle.backend.events.model.*;
import com.cornercircle.backend.events.repository.EventRepository;
import com.cornercircle.backend.membership.model.MembershipStatus;
import com.cornercircle.backend.membership.repository.MembershipApplicationRepository;
import com.cornercircle.backend.registration.model.*;
import com.cornercircle.backend.registration.repository.EventRegistrationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
@Service public class AdminDashboardService {
 private final MembershipApplicationRepository members; private final EventRegistrationRepository registrations; private final EventRepository events;
 public AdminDashboardService(MembershipApplicationRepository members,EventRegistrationRepository registrations,EventRepository events){this.members=members;this.registrations=registrations;this.events=events;}
 @Transactional(readOnly=true) public AdminDashboardResponse get(){
  LocalDate today=LocalDate.now(); LocalDateTime monthStart=today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(); LocalDateTime nextMonth=today.plusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
  var allMembers=members.findAll(Sort.by(Sort.Direction.DESC,"createdAt")); var allRegistrations=registrations.findAll(Sort.by(Sort.Direction.DESC,"createdAt"));
  var confirmed=allRegistrations.stream().filter(item->item.getStatus()==EventRegistrationStatus.CONFIRMED).toList(); var upcoming=events.findUpcomingEvents(EventPublicationStatus.PUBLISHED,today,LocalTime.now());
  long activeMembers=allMembers.stream().filter(item->item.getStatus()==MembershipStatus.ACTIVE).filter(item->item.getMembershipEndsOn()==null||!item.getMembershipEndsOn().isBefore(today)).count();
  BigDecimal revenue=confirmed.stream().filter(item->item.getConfirmedAt()!=null&&!item.getConfirmedAt().isBefore(monthStart)&&item.getConfirmedAt().isBefore(nextMonth)).map(EventRegistration::getTotalAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
  var upcomingItems=upcoming.stream().limit(3).map(item->new AdminDashboardResponse.UpcomingEvent(item.getId(),item.getTitle(),item.getSlug(),item.getEventDate(),item.getStartTime(),item.getCapacity(),registrations.reservedSeats(item.getId(),List.of(EventRegistrationStatus.CONFIRMED)))).toList();
  var attention=new ArrayList<AdminDashboardResponse.AttentionItem>(); long incomplete=upcoming.stream().filter(this::isIncomplete).count();
  if(incomplete>0)attention.add(new AdminDashboardResponse.AttentionItem("EVENT_DETAILS",incomplete+(incomplete==1?" event needs details":" events need details"),"Add missing time, location, capacity, or description.","/admin/events","Update events"));
  long expiring=allMembers.stream().filter(item->item.getStatus()==MembershipStatus.ACTIVE&&item.getMembershipEndsOn()!=null).filter(item->!item.getMembershipEndsOn().isBefore(today)&&!item.getMembershipEndsOn().isAfter(today.plusDays(60))).count();
  if(expiring>0)attention.add(new AdminDashboardResponse.AttentionItem("MEMBERSHIP_EXPIRY",expiring+(expiring==1?" membership expires soon":" memberships expire soon"),"Memberships ending within the next 60 days.","/admin/members","View members"));
  long nearlyFull=upcoming.stream().filter(item->item.getCapacity()!=null&&item.getCapacity()>0).filter(item->registrations.reservedSeats(item.getId(),List.of(EventRegistrationStatus.CONFIRMED))>=Math.ceil(item.getCapacity()*.8)).count();
  if(nearlyFull>0)attention.add(new AdminDashboardResponse.AttentionItem("EVENT_CAPACITY",nearlyFull+(nearlyFull==1?" event is nearly full":" events are nearly full"),"At least 80% of available places are reserved.","/admin/event-registrations","View registrations"));
  var activity=new ArrayList<AdminDashboardResponse.ActivityItem>(); confirmed.forEach(item->activity.add(new AdminDashboardResponse.ActivityItem("REGISTRATION",item.getFullName()+" registered for "+item.getEvent().getTitle(),item.getConfirmedAt()==null?item.getCreatedAt():item.getConfirmedAt(),"/admin/event-registrations")));
  allMembers.stream().filter(item->item.getStatus()==MembershipStatus.ACTIVE).forEach(item->activity.add(new AdminDashboardResponse.ActivityItem("MEMBER",item.getFullName()+" joined as a new member",item.getPaidAt()==null?item.getCreatedAt():item.getPaidAt(),"/admin/members"))); activity.sort(Comparator.comparing(AdminDashboardResponse.ActivityItem::occurredAt).reversed());
  return new AdminDashboardResponse(new AdminDashboardResponse.Summary(activeMembers,confirmed.size(),upcoming.size(),revenue),upcomingItems,attention,activity.stream().limit(6).toList());
 }
 private boolean isIncomplete(Event item){return item.getEventDate()==null||item.getStartTime()==null||item.getEndTime()==null||item.getVenueName()==null||item.getVenueName().isBlank()||item.getCapacity()==null||item.getDescription()==null||item.getDescription().isBlank();}
}
