package org.ngolibrary.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/** A photo of a user's Aadhaar card, kept in its own table so it is only loaded when staff open it. */
@Entity
@Table(name = "aadhaar_photos")
public class AadhaarPhoto {

    @Id
    private Long userId;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
