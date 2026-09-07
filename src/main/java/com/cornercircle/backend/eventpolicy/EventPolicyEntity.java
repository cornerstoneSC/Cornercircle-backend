package com.cornercircle.backend.eventpolicy;

import jakarta.persistence.*;

@Entity
@Table(name="event_policy_content")
public class EventPolicyEntity {
    @Id private Long id = 1L;
    @Column(nullable=false, columnDefinition="TEXT") private String content;
    protected EventPolicyEntity() {}
    public EventPolicyEntity(String content) { this.content = content; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
