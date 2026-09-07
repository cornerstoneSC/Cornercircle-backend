package com.cornercircle.backend.homepage.dto;

public class HomepageResponse {
    private long id;
    private String heroImageUrl;
    private String heroImagePublicId;
    private String aboutImageUrl;
    private String aboutImagePublicId;
    private String beliefsImageUrl;
    private String beliefsImagePublicId;
    private String founderImageUrl;
    private String founderImagePublicId;
    private String contentJson;

    public String getBeliefsImageUrl() { return beliefsImageUrl; }
    public String getBeliefsImagePublicId() { return beliefsImagePublicId; }
    public String getContentJson() { return contentJson; }
    public String getFounderImageUrl() { return founderImageUrl; }
    public String getFounderImagePublicId() { return founderImagePublicId; }
    public void setFounderImageUrl(String value) { founderImageUrl = value; }
    public void setFounderImagePublicId(String value) { founderImagePublicId = value; }

    public HomepageResponse(long id, String heroImageUrl, String heroImagePublicId,
            String aboutImageUrl, String aboutImagePublicId,
            String beliefsImageUrl, String beliefsImagePublicId) {
        this(id, heroImageUrl, heroImagePublicId, aboutImageUrl, aboutImagePublicId);
        this.beliefsImageUrl = beliefsImageUrl;
        this.beliefsImagePublicId = beliefsImagePublicId;
    }

    public void setContentJson(String value) { contentJson = value; }

    public HomepageResponse(
            long id,
            String heroImageUrl,
            String heroImagePublicId,
            String aboutImageUrl,
            String aboutImagePublicId
    ) {
        this.id = id;
        this.heroImageUrl = heroImageUrl;
        this.heroImagePublicId = heroImagePublicId;
        this.aboutImageUrl = aboutImageUrl;
        this.aboutImagePublicId = aboutImagePublicId;
    }

    public long getId() {
        return id;
    }

    public String getHeroImageUrl() {
        return heroImageUrl;
    }

    public String getHeroImagePublicId() {
        return heroImagePublicId;
    }

    public String getAboutImageUrl() {
        return aboutImageUrl;
    }

    public String getAboutImagePublicId() {
        return aboutImagePublicId;
    }
}
