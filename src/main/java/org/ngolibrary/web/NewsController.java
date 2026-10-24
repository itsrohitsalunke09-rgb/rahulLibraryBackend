package org.ngolibrary.web;

import org.ngolibrary.domain.NewsPhoto;
import org.ngolibrary.dto.NewsStoryRequest;
import org.ngolibrary.dto.NewsStoryResponse;
import org.ngolibrary.service.AuthService;
import org.ngolibrary.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** News and events. Anyone signed in can read; staff post, edit and remove. */
@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;
    private final AuthService authService;

    public NewsController(NewsService newsService, AuthService authService) {
        this.newsService = newsService;
        this.authService = authService;
    }

    @GetMapping
    public Page<NewsStoryResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String q) {
        return newsService.list(page, size, q);
    }

    @GetMapping("/{id}")
    public NewsStoryResponse get(@PathVariable Long id) {
        return newsService.get(id);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        NewsPhoto photo = newsService.photo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=31536000")
                .body(photo.getData());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public NewsStoryResponse create(@RequestPart("story") NewsStoryRequest story,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        return newsService.create(story, photo, authService.current(authentication));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public NewsStoryResponse update(@PathVariable Long id, @RequestPart("story") NewsStoryRequest story,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return newsService.update(id, story, photo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void delete(@PathVariable Long id) {
        newsService.delete(id);
    }
}
