package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ngolibrary.domain.NewsPhoto;
import org.ngolibrary.domain.NewsStory;
import org.ngolibrary.domain.NewsTemplate;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.NewsStoryRequest;
import org.ngolibrary.dto.NewsStoryResponse;
import org.ngolibrary.repo.NewsPhotoRepository;
import org.ngolibrary.repo.NewsStoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class NewsServiceTest {

    private NewsStoryRepository stories;
    private NewsPhotoRepository photos;
    private NewsService service;
    private UserAccount author;

    @BeforeEach
    void setUp() {
        stories = mock(NewsStoryRepository.class);
        photos = mock(NewsPhotoRepository.class);
        when(stories.save(any(NewsStory.class))).thenAnswer(inv -> {
            NewsStory s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(1L);
            }
            return s;
        });
        service = new NewsService(stories, photos);
        author = new UserAccount();
        author.setFullName("Asha Patil");
    }

    private static NewsStoryRequest request(String headline, String body, LocalDate date) {
        NewsStoryRequest r = new NewsStoryRequest();
        r.setHeadline(headline);
        r.setBody(body);
        r.setEventDate(date);
        return r;
    }

    private static NewsStoryRequest valid() {
        return request("Book fair", "We held a fair.", LocalDate.of(2026, 10, 1));
    }

    private static void assertBadRequest(String message, Runnable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(message, ex.getReason());
    }

    private static MockMultipartFile jpeg(int size) {
        return new MockMultipartFile("photo", "p.jpg", "image/jpeg", new byte[size]);
    }

    private NewsStory existing(boolean hasPhoto, int version) {
        NewsStory s = new NewsStory();
        s.setId(7L);
        s.setHeadline("Old");
        s.setBody("Old body");
        s.setEventDate(LocalDate.of(2026, 9, 1));
        s.setHasPhoto(hasPhoto);
        s.setPhotoVersion(version);
        s.setPhotoCaption(hasPhoto ? "Old caption" : null);
        when(stories.findById(7L)).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void createRejectsBlankHeadline() {
        assertBadRequest("Headline is required",
                () -> service.create(request("  ", "b", LocalDate.now()), null, author));
    }

    @Test
    void createRejectsBlankBody() {
        assertBadRequest("Story text is required",
                () -> service.create(request("h", " ", LocalDate.now()), null, author));
    }

    @Test
    void createRejectsMissingDate() {
        assertBadRequest("Event date is required", () -> service.create(request("h", "b", null), null, author));
    }

    @Test
    void createRejectsLongBody() {
        assertBadRequest("Story text can be at most 10000 characters",
                () -> service.create(request("h", "x".repeat(10_001), LocalDate.now()), null, author));
        NewsStoryResponse ok = service.create(request("h", "x".repeat(10_000), LocalDate.now()), null, author);
        assertEquals(10_000, ok.body().length());
    }

    @Test
    void createRejectsLongHeadline() {
        assertBadRequest("Headline can be at most 150 characters",
                () -> service.create(request("h".repeat(151), "b", LocalDate.now()), null, author));
    }

    @Test
    void createRejectsBigPhoto() {
        assertBadRequest("Photo must be under 2 MB",
                () -> service.create(valid(), jpeg(2 * 1024 * 1024 + 1), author));
    }

    @Test
    void createRejectsNonImage() {
        MockMultipartFile pdf = new MockMultipartFile("photo", "a.pdf", "application/pdf", new byte[10]);
        assertBadRequest("Photo must be a JPEG, PNG or WebP image", () -> service.create(valid(), pdf, author));
    }

    @Test
    void createWithPhotoSavesPhotoAndSetsVersion1() {
        byte[] bytes = {1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("photo", "p.jpg", "image/jpeg", bytes);
        NewsStoryRequest req = valid();
        req.setPhotoCaption(" A caption ");
        NewsStoryResponse res = service.create(req, file, author);
        assertTrue(res.hasPhoto());
        assertEquals(1, res.photoVersion());
        assertEquals("Asha Patil", res.createdByName());
        assertEquals("A caption", res.photoCaption());
        ArgumentCaptor<NewsPhoto> cap = ArgumentCaptor.forClass(NewsPhoto.class);
        verify(photos).save(cap.capture());
        assertEquals(1L, cap.getValue().getStoryId());
        assertEquals("image/jpeg", cap.getValue().getContentType());
        assertTrue(java.util.Arrays.equals(bytes, cap.getValue().getData()));
    }

    @Test
    void updateWithoutFileKeepsPhoto() {
        existing(true, 3);
        NewsStoryResponse res = service.update(7L, valid(), null);
        assertTrue(res.hasPhoto());
        assertEquals(3, res.photoVersion());
        verify(photos, never()).deleteById(any());
        verify(photos, never()).save(any());
    }

    @Test
    void updateReplacingPhotoBumpsVersion() {
        existing(true, 3);
        NewsStoryResponse res = service.update(7L, valid(), jpeg(10));
        assertTrue(res.hasPhoto());
        assertEquals(4, res.photoVersion());
        verify(photos).save(any(NewsPhoto.class));
    }

    @Test
    void updateRemovePhotoClearsCaption() {
        existing(true, 3);
        NewsStoryRequest req = valid();
        req.setRemovePhoto(true);
        req.setPhotoCaption("still here");
        NewsStoryResponse res = service.update(7L, req, null);
        assertFalse(res.hasPhoto());
        assertNull(res.photoCaption());
        verify(photos).deleteById(7L);
    }

    @Test
    void captionClearedWhenNoPhoto() {
        NewsStoryRequest req = valid();
        req.setPhotoCaption("orphan caption");
        assertNull(service.create(req, null, author).photoCaption());
    }

    @Test
    void updateUnknownIdIs404() {
        when(stories.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(99L, valid(), null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Story not found", ex.getReason());
    }

    @Test
    void createDefaultsToClassic() {
        assertEquals("CLASSIC", service.create(valid(), null, author).template());
        NewsStoryRequest blank = valid();
        blank.setTemplate("");
        assertEquals("CLASSIC", service.create(blank, null, author).template());
    }

    @Test
    void createStoresTemplate() {
        NewsStoryRequest req = valid();
        req.setTemplate(" notice ");
        assertEquals("NOTICE", service.create(req, null, author).template());
        ArgumentCaptor<NewsStory> cap = ArgumentCaptor.forClass(NewsStory.class);
        verify(stories).save(cap.capture());
        assertEquals(NewsTemplate.NOTICE, cap.getValue().getTemplate());
    }

    @Test
    void unknownTemplateIs400() {
        NewsStoryRequest req = valid();
        req.setTemplate("POSTER");
        assertBadRequest("Unknown template", () -> service.create(req, null, author));
    }

    @Test
    void nullTemplateReadsAsClassic() {
        NewsStory s = existing(false, 0);
        s.setTemplate(null);
        assertEquals("CLASSIC", service.get(7L).template());
    }

    @Test
    void updateTemplateKeepsPhoto() {
        existing(true, 2);
        NewsStoryRequest req = valid();
        req.setTemplate("PHOTO_BANNER");
        NewsStoryResponse res = service.update(7L, req, null);
        assertTrue(res.hasPhoto());
        assertEquals(2, res.photoVersion());
        assertEquals("PHOTO_BANNER", res.template());
    }
}
