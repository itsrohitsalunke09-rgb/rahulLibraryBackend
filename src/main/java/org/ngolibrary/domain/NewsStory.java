package org.ngolibrary.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** A news / event story shown on the News and events page. The photo lives in {@link NewsPhoto}. */
@Entity
@Table(name = "news_stories")
public class NewsStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String headline;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(length = 200)
    private String photoCaption;

    @Column(nullable = false)
    private boolean hasPhoto;

    /** Bumped whenever the photo is replaced, so clients can bust their image cache. */
    @Column(nullable = false)
    private int photoVersion;

    @Column(length = 200)
    private String createdByName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Layout template; null on rows created before templates existed (read as CLASSIC). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NewsTemplate template;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public boolean isHasPhoto() {
        return hasPhoto;
    }

    public void setHasPhoto(boolean hasPhoto) {
        this.hasPhoto = hasPhoto;
    }

    public int getPhotoVersion() {
        return photoVersion;
    }

    public void setPhotoVersion(int photoVersion) {
        this.photoVersion = photoVersion;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public NewsTemplate getTemplate() {
        return template;
    }

    public void setTemplate(NewsTemplate template) {
        this.template = template;
    }
}
