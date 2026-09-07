package com.cornercircle.backend.dashboard.dto;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public record AdminDashboardResponse(Summary summary,List<UpcomingEvent> upcomingEvents,List<AttentionItem> attentionItems,List<ActivityItem> recentActivity) {
 public record Summary(long activeMembers,long paidEventRegistrations,long upcomingEvents,BigDecimal revenueThisMonth){}
 public record UpcomingEvent(Long id,String title,String slug,LocalDate date,LocalTime startTime,Integer capacity,long registered){}
 public record AttentionItem(String type,String title,String description,String href,String actionLabel){}
 public record ActivityItem(String type,String description,LocalDateTime occurredAt,String href){}
}
