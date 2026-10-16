package org.ngolibrary.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/** The photo bytes for a {@link NewsStory}, kept in their own table so story lists stay light. */
@Entity
@Table(name = "news_photos")
public class NewsPhoto {

    @Id
    private Long storyId;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public Long getStoryId() {
        return storyId;
    }

    public void setStoryId(Long storyId) {
        this.storyId = storyId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
