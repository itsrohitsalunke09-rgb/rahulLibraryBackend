package org.ngolibrary.web;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.ngolibrary.domain.IssueStatus;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.ApiMessage;
import org.ngolibrary.dto.DashboardResponse;
import org.ngolibrary.dto.IssueRequest;
import org.ngolibrary.dto.IssueResponse;
import org.ngolibrary.service.AuthService;
import org.ngolibrary.service.IssueService;
import org.ngolibrary.service.OverdueNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IssueController {

    private final IssueService issueService;
    private final AuthService authService;
    private final OverdueNotificationService overdueNotificationService;

    public IssueController(IssueService issueService, AuthService authService, OverdueNotificationService overdueNotificationService) {
        this.issueService = issueService;
        this.authService = authService;
        this.overdueNotificationService = overdueNotificationService;
    }

    @GetMapping("/issues")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Page<IssueResponse> all(Pageable pageable, @RequestParam(required = false) IssueStatus status, @RequestParam(required = false) String q) {
        return issueService.allPaged(pageable, status, q);
    }

    @GetMapping("/issues/count")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public long count(@RequestParam(required = false) IssueStatus status, @RequestParam(required = false) String q) {
        return issueService.countByStatus(status, q);
    }

    @GetMapping("/issues/counts")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Map<String, Long> counts(@RequestParam(required = false) String q) {
        return issueService.counts(q);
    }

    @GetMapping("/issues/issued")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Page<IssueResponse> issued(Pageable pageable) {
        return issueService.issuedPaged(pageable);
    }

    @GetMapping("/issues/overdue")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Page<IssueResponse> overdue(Pageable pageable) {
        return issueService.overduePaged(pageable);
    }

    @GetMapping("/issues/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<IssueResponse> mine(Authentication authentication) {
        UserAccount student = authService.current(authentication.getName());
        return issueService.mine(student);
    }

    @GetMapping("/issues/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public Page<IssueResponse> minePaged(Pageable pageable, Authentication authentication) {
        UserAccount student = authService.current(authentication.getName());
        return issueService.minePaged(student, pageable);
    }

    @PostMapping("/issues")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public IssueResponse issue(@Valid @RequestBody IssueRequest request, Authentication authentication) {
        UserAccount librarian = authService.current(authentication.getName());
        return issueService.issue(request, librarian);
    }

    @PostMapping("/issues/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public IssueResponse returnBook(@PathVariable Long id) {
        return issueService.returnBook(id);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public DashboardResponse dashboard() {
        return issueService.dashboard();
    }

    @GetMapping("/books/{bookId}/issues")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public List<IssueResponse> getBookIssues(@PathVariable Long bookId) {
        return issueService.getByBookId(bookId);
    }

    @PostMapping("/overdue/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiMessage triggerOverdueCheck() {
        overdueNotificationService.checkAndNotifyOverdue();
        return new ApiMessage("Overdue check triggered successfully");
    }
}
