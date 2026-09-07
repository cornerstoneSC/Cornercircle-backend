package com.cornercircle.backend.homepage.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;

@Entity
@Table(name = "homepage_content")
public class HomepageContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String heroImageUrl;
    private String heroImagePublicId;
    private String aboutImageUrl;
    private String aboutImagePublicId;
    private String beliefsImageUrl;
    private String beliefsImagePublicId;
    private String founderImageUrl;
    private String founderImagePublicId;
    private String newsletterImageUrl;
    private String newsletterImagePublicId;

    @Lob
    @Column(name = "content_json", columnDefinition = "TEXT")
    private String contentJson;

    public String getContentJson() { return contentJson; }
    public void setContentJson(String value) { contentJson = value; }

    public String getBeliefsImageUrl() { return beliefsImageUrl; }
    public void setBeliefsImageUrl(String value) { beliefsImageUrl = value; }
    public String getBeliefsImagePublicId() { return beliefsImagePublicId; }
    public void setBeliefsImagePublicId(String value) { beliefsImagePublicId = value; }
    public String getFounderImageUrl() { return founderImageUrl; }
    public void setFounderImageUrl(String value) { founderImageUrl = value; }
    public String getFounderImagePublicId() { return founderImagePublicId; }
    public void setFounderImagePublicId(String value) { founderImagePublicId = value; }
    public String getNewsletterImageUrl() { return newsletterImageUrl; }
    public void setNewsletterImageUrl(String value) { newsletterImageUrl = value; }
    public String getNewsletterImagePublicId() { return newsletterImagePublicId; }
    public void setNewsletterImagePublicId(String value) { newsletterImagePublicId = value; }

    @Column(name = "upcoming_events_enabled", nullable = false)
    private boolean upcomingEventsEnabled = true;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHeroImageUrl() {
        return heroImageUrl;
    }

    public void setHeroImageUrl(String heroImageUrl) {
        this.heroImageUrl = heroImageUrl;
    }

    public String getHeroImagePublicId() {
        return heroImagePublicId;
    }

    public void setHeroImagePublicId(String heroImagePublicId) {
        this.heroImagePublicId = heroImagePublicId;
    }

    public boolean isUpcomingEventsEnabled() {
        return upcomingEventsEnabled;
    }

    public void setUpcomingEventsEnabled(boolean upcomingEventsEnabled) {
        this.upcomingEventsEnabled = upcomingEventsEnabled;
    }

    public String getAboutImageUrl() {
        return aboutImageUrl;
    }

    public void setAboutImageUrl(String aboutImageUrl) {
        this.aboutImageUrl = aboutImageUrl;
    }

    public String getAboutImagePublicId() {
        return aboutImagePublicId;
    }

    public void setAboutImagePublicId(String aboutImagePublicId) {
        this.aboutImagePublicId = aboutImagePublicId;
    }
}
