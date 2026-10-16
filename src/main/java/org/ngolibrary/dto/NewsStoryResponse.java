package org.ngolibrary.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.ngolibrary.domain.NewsStory;

public record NewsStoryResponse(
        Long id,
        String headline,
        String body,
        LocalDate eventDate,
        String photoCaption,
        boolean hasPhoto,
        int photoVersion,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static NewsStoryResponse from(NewsStory s) {
        return new NewsStoryResponse(s.getId(), s.getHeadline(), s.getBody(), s.getEventDate(),
                s.getPhotoCaption(), s.isHasPhoto(), s.getPhotoVersion(), s.getCreatedByName(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
