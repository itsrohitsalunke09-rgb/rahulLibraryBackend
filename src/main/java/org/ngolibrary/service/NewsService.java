package org.ngolibrary.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

import org.ngolibrary.domain.NewsStory;
import org.ngolibrary.domain.NewsTemplate;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.NewsStoryRequest;
import org.ngolibrary.dto.NewsStoryResponse;
import org.ngolibrary.repo.NewsStoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** News and events stories, each with an optional photo kept in photo storage. */
@Service
public class NewsService {

    public static final int MAX_HEADLINE = 150;
    public static final int MAX_BODY = 10_000;
    public static final int MAX_CAPTION = 200;
    public static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;
    public static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final NewsStoryRepository stories;
    private final PhotoStorage photos;

    public NewsService(NewsStoryRepository stories, PhotoStorage photos) {
        this.stories = stories;
        this.photos = photos;
    }

    /** Where a story's photo is stored. */
    public static String photoPath(Long storyId) {
        return "news/" + storyId;
    }

    @Transactional(readOnly = true)
    public Page<NewsStoryResponse> list(int page, int size, String q) {
        PageRequest paging = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<NewsStory> found = q == null || q.isBlank()
                ? stories.findAllByOrderByEventDateDescIdDesc(paging)
                : stories.findByHeadlineContainingIgnoreCaseOrderByEventDateDescIdDesc(q.trim(), paging);
        return found.map(NewsStoryResponse::from);
    }

    @Transactional(readOnly = true)
    public NewsStoryResponse get(Long id) {
        return NewsStoryResponse.from(find(id));
    }

    @Transactional
    public NewsStoryResponse create(NewsStoryRequest req, MultipartFile photo, UserAccount author) {
        validate(req);
        NewsTemplate template = parseTemplate(req.getTemplate());
        boolean hasFile = photo != null && !photo.isEmpty();
        if (hasFile) {
            checkPhoto(photo);
        }
        LocalDateTime now = LocalDateTime.now();
        NewsStory story = new NewsStory();
        story.setCreatedByName(author.getFullName());
        story.setCreatedAt(now);
        story.setUpdatedAt(now);
        applyText(story, req);
        story.setTemplate(template);
        story.setHasPhoto(hasFile);
        story.setPhotoVersion(hasFile ? 1 : 0);
        if (!hasFile) {
            story.setPhotoCaption(null);
        }
        NewsStory saved = stories.save(story);
        if (hasFile) {
            storePhoto(saved.getId(), photo);
        }
        return NewsStoryResponse.from(saved);
    }

    @Transactional
    public NewsStoryResponse update(Long id, NewsStoryRequest req, MultipartFile photo) {
        NewsStory story = find(id);
        validate(req);
        // An edit that sends no template keeps the story's own; an unknown one is still rejected.
        NewsTemplate template = trimmed(req.getTemplate()) == null && story.getTemplate() != null
                ? story.getTemplate()
                : parseTemplate(req.getTemplate());
        boolean hasFile = photo != null && !photo.isEmpty();
        if (hasFile) {
            checkPhoto(photo);
        }
        applyText(story, req);
        story.setTemplate(template);
        story.setUpdatedAt(LocalDateTime.now());
        if (hasFile) {
            story.setHasPhoto(true);
            story.setPhotoVersion(story.getPhotoVersion() + 1);
            storePhoto(id, photo);
        } else if (req.isRemovePhoto() && story.isHasPhoto()) {
            story.setHasPhoto(false);
            photos.delete(photoPath(id));
        }
        if (!story.isHasPhoto()) {
            story.setPhotoCaption(null);
        }
        return NewsStoryResponse.from(stories.save(story));
    }

    @Transactional
    public void delete(Long id) {
        NewsStory story = find(id);
        if (story.isHasPhoto()) {
            photos.delete(photoPath(id));
        }
        stories.delete(story);
    }

    public PhotoStorage.Photo photo(Long id) {
        return photos.get(photoPath(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
    }

    private NewsStory find(Long id) {
        return stories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
    }

    private void validate(NewsStoryRequest req) {
        String headline = trimmed(req.getHeadline());
        String body = trimmed(req.getBody());
        String caption = trimmed(req.getPhotoCaption());
        if (headline == null) {
            throw bad("Headline is required");
        }
        if (headline.length() > MAX_HEADLINE) {
            throw bad("Headline can be at most " + MAX_HEADLINE + " characters");
        }
        if (body == null) {
            throw bad("Story text is required");
        }
        if (body.length() > MAX_BODY) {
            throw bad("Story text can be at most " + MAX_BODY + " characters");
        }
        if (req.getEventDate() == null) {
            throw bad("Event date is required");
        }
        if (caption != null && caption.length() > MAX_CAPTION) {
            throw bad("Photo caption can be at most " + MAX_CAPTION + " characters");
        }
    }

    private void applyText(NewsStory story, NewsStoryRequest req) {
        story.setHeadline(trimmed(req.getHeadline()));
        story.setBody(trimmed(req.getBody()));
        story.setEventDate(req.getEventDate());
        story.setPhotoCaption(trimmed(req.getPhotoCaption()));
    }

    private static NewsTemplate parseTemplate(String value) {
        String t = trimmed(value);
        if (t == null) {
            return NewsTemplate.CLASSIC;
        }
        try {
            return NewsTemplate.valueOf(t.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw bad("Unknown template");
        }
    }

    private void checkPhoto(MultipartFile photo) {
        if (photo.getSize() > MAX_PHOTO_BYTES) {
            throw bad("Photo must be under 2 MB");
        }
        String type = photo.getContentType();
        if (type == null || !PHOTO_TYPES.contains(type.toLowerCase())) {
            throw bad("Photo must be a JPEG, PNG or WebP image");
        }
    }

    private void storePhoto(Long storyId, MultipartFile file) {
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw bad("The photo could not be read. Please try again.");
        }
        photos.put(photoPath(storyId), file.getContentType().toLowerCase(), data);
    }

    private static String trimmed(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
