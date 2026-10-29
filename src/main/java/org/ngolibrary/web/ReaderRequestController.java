package org.ngolibrary.web;

import java.util.List;

import org.ngolibrary.domain.ReaderRequest;
import org.ngolibrary.service.AuthService;
import org.ngolibrary.service.ReaderRequestService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Students request catalog books and suggest new ones; staff see them on the Dashboard and clear them. */
@RestController
@RequestMapping("/api/reader-requests")
public class ReaderRequestController {

    public record Suggestion(String title, String author, String note) {
    }

    private final ReaderRequestService service;
    private final AuthService authService;

    public ReaderRequestController(ReaderRequestService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public List<ReaderRequest> list() {
        return service.list();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ReaderRequest> mine(Authentication authentication) {
        return service.mine(authService.current(authentication));
    }

    @PostMapping("/books/{bookId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ReaderRequest request(@PathVariable Long bookId, Authentication authentication) {
        return service.request(bookId, authService.current(authentication));
    }

    @PostMapping("/suggestions")
    @PreAuthorize("hasRole('STUDENT')")
    public ReaderRequest suggest(@RequestBody Suggestion suggestion, Authentication authentication) {
        return service.suggest(suggestion.title(), suggestion.author(), suggestion.note(), authService.current(authentication));
    }

    @PutMapping("/{id}/done")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('DONE')")
    public ReaderRequest done(@PathVariable Long id) {
        return service.done(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('DELETE')")
    public void clear(@PathVariable Long id) {
        service.clear(id);
    }
}
