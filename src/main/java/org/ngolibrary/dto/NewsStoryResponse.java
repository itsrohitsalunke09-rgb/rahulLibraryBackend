package org.ngolibrary.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.ngolibrary.domain.NewsStory;
import org.ngolibrary.domain.NewsTemplate;

public record NewsStoryResponse(
        Long id,
        String headline,
        String body,
        LocalDate eventDate,
        String photoCaption,
        String template,
        boolean hasPhoto,
        int photoVersion,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static NewsStoryResponse from(NewsStory s) {
        return new NewsStoryResponse(s.getId(), s.getHeadline(), s.getBody(), s.getEventDate(),
                s.getPhotoCaption(),
                (s.getTemplate() == null ? NewsTemplate.CLASSIC : s.getTemplate()).name(), s.isHasPhoto(), s.getPhotoVersion(), s.getCreatedByName(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
