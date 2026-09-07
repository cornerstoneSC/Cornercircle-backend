package com.cornercircle.backend.membershippage;

import jakarta.persistence.*;

@Entity
@Table(name="membership_page_content")
public class MembershipPageEntity {
    @Id private Long id = 1L;
    @Column(nullable=false, columnDefinition="TEXT") private String content;
    protected MembershipPageEntity() {}
    public MembershipPageEntity(String content) { this.content = content; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
