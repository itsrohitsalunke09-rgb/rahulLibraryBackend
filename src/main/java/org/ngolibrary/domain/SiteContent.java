package org.ngolibrary.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/** Page content edited by staff, stored as one JSON document per page (e.g. "about"). */
@Entity
@Table(name = "site_content")
public class SiteContent {

    @Id
    @Column(length = 40)
    private String page;

    @Column(nullable = false, columnDefinition = "text")
    private String json;

    private LocalDateTime updatedAt;

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
