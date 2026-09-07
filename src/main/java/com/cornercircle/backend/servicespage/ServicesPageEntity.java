package com.cornercircle.backend.servicespage;
import jakarta.persistence.*;
@Entity
@Table(name="services_page_content")
public class ServicesPageEntity {
    @Id private Long id = 1L;
    @Column(nullable=false, columnDefinition="TEXT") private String content;
    protected ServicesPageEntity() {}
    public ServicesPageEntity(String content) { this.content = content; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
