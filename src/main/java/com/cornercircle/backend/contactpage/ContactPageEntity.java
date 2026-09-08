package com.cornercircle.backend.contactpage;
import jakarta.persistence.*;
@Entity
@Table(name="contact_page_content")
public class ContactPageEntity {
    @Id private Long id = 1L;
    @Column(nullable=false, columnDefinition="TEXT") private String content;
    protected ContactPageEntity() {}
    public ContactPageEntity(String content) { this.content = content; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
