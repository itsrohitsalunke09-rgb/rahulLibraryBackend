package org.ngolibrary.web;

import java.time.YearMonth;
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
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public Page<IssueResponse> all(Pageable pageable, @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) String q, @RequestParam(required = false) Integer year) {
        if (year != null) {
            // Year filter: issues made in that year (by issue date), optionally of one status.
            return issueService.issuedInYear(year, status, pageable);
        }
        return issueService.allPaged(pageable, status, q);
    }

    /** Years to offer in the Issues year filter, latest first. */
    @GetMapping("/issues/years")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public List<Integer> years() {
        return issueService.issueYears();
    }

    /** One student's books: given in the year, or all years without one. */
    @GetMapping("/issues/student/{studentId}")
    @PreAuthorize("(hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')) or @perm.can('GALAXY_VIEW')")
    public List<IssueResponse> studentYear(@PathVariable Long studentId, @RequestParam(required = false) Integer year) {
        return issueService.studentYear(studentId, year);
    }

    /** Books report student list: each student's totals by status across all years. */
    @GetMapping("/issues/students")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public List<org.ngolibrary.dto.StudentSummary> students() {
        return issueService.studentSummaries();
    }

    /** Books report circles: books given to each student in the year. */
    @GetMapping("/issues/readers")
    @PreAuthorize("@perm.can('GALAXY_VIEW')")
    public List<org.ngolibrary.dto.ReaderCount> readers(@RequestParam int year) {
        return issueService.readersInYear(year);
    }

    /** Book transactions report: issues given or returned in a month (YYYY-MM). */
    @GetMapping("/issues/month")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public List<IssueResponse> month(@RequestParam YearMonth month) {
        return issueService.movedInMonth(month);
    }

    @GetMapping("/issues/count")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public long count(@RequestParam(required = false) IssueStatus status, @RequestParam(required = false) String q) {
        return issueService.countByStatus(status, q);
    }

    @GetMapping("/issues/counts")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public Map<String, Long> counts(@RequestParam(required = false) String q, @RequestParam(required = false) Integer year) {
        return issueService.counts(q, year);
    }

    @GetMapping("/issues/issued")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public Page<IssueResponse> issued(Pageable pageable, @RequestParam(required = false) Integer year) {
        return issueService.issuedPaged(pageable, year);
    }

    @GetMapping("/issues/overdue")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public Page<IssueResponse> overdue(Pageable pageable) {
        return issueService.overduePaged(pageable);
    }

    @GetMapping("/issues/mine")
    @PreAuthorize("hasRole('STUDENT') and @perm.can('READ')")
    public Page<IssueResponse> mine(Pageable pageable, Authentication authentication) {
        UserAccount student = authService.current(authentication);
        return issueService.minePaged(student, pageable);
    }

    @PostMapping("/issues")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('WRITE')")
    public IssueResponse issue(@Valid @RequestBody IssueRequest request, Authentication authentication) {
        UserAccount librarian = authService.current(authentication);
        return issueService.issue(request, librarian);
    }

    @PostMapping("/issues/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('UPDATE')")
    public IssueResponse returnBook(@PathVariable Long id) {
        return issueService.returnBook(id);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
    public DashboardResponse dashboard(@RequestParam(required = false) Integer year) {
        // With a year, out/overdue count only issues made in that year.
        return issueService.dashboard(year);
    }

    @GetMapping("/books/{bookId}/issues")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN') and @perm.can('READ')")
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
