package org.ngolibrary.dto;

import java.time.LocalDate;

/** Fields of a news story form; validated by NewsService so messages stay readable. */
public class NewsStoryRequest {
    private String headline;
    private String body;
    private LocalDate eventDate;
    private String photoCaption;
    private String template;
    private boolean removePhoto;

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getPhotoCaption() {
        return photoCaption;
    }

    public void setPhotoCaption(String photoCaption) {
        this.photoCaption = photoCaption;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean isRemovePhoto() {
        return removePhoto;
    }

    public void setRemovePhoto(boolean removePhoto) {
        this.removePhoto = removePhoto;
    }
}
