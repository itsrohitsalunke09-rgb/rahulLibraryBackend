package org.ngolibrary.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ngolibrary.domain.Book;
import org.ngolibrary.domain.BookIssue;
import org.ngolibrary.domain.IssueStatus;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.DashboardResponse;
import org.ngolibrary.dto.IssueRequest;
import org.ngolibrary.dto.IssueResponse;
import org.ngolibrary.repo.BookIssueRepository;
import org.ngolibrary.repo.BookRepository;
import org.ngolibrary.repo.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IssueService {

    private final BookIssueRepository issues;
    private final BookRepository books;
    private final UserAccountRepository users;

    public IssueService(BookIssueRepository issues, BookRepository books, UserAccountRepository users) {
        this.issues = issues;
        this.books = books;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> allPaged(Pageable pageable, IssueStatus status, String q) {
        if (q != null && !q.trim().isEmpty()) {
            return issues.searchByBookTitleOrStudentName(q.trim(), pageable).map(IssueResponse::from);
        }
        if (status == null) {
            return issues.findAll(pageable).map(IssueResponse::from);
        }
        if (status == IssueStatus.ISSUED) {
            return issues.findByStatusAndDueDateAfterOrEqual(IssueStatus.ISSUED, LocalDate.now(), pageable).map(IssueResponse::from);
        }
        if (status == IssueStatus.OVERDUE) {
            return issues.findByStatusAndDueDateBeforeOrderByIssueDateDesc(IssueStatus.ISSUED, LocalDate.now(), pageable).map(IssueResponse::from);
        }
        return issues.findByStatusPaged(status, pageable).map(IssueResponse::from);
    }

    /**
     * Issues made in the given year (by issue date), newest first. With a status the list is
     * narrowed the same way as the unfiltered list: ISSUED = not yet due, OVERDUE = past due,
     * RETURNED = returned; no status = every issue of that year.
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> issuedInYear(int year, IssueStatus status, Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year + 1, 1, 1);
        Page<BookIssue> page;
        if (status == null) {
            page = issues.findIssuedBetween(from, to, unsorted);
        } else if (status == IssueStatus.ISSUED || status == IssueStatus.OVERDUE) {
            page = issues.findByDueIssuedBetween(IssueStatus.ISSUED, status == IssueStatus.OVERDUE, LocalDate.now(),
                    from, to, unsorted);
        } else {
            page = issues.findByStatusIssuedBetween(status, from, to, unsorted);
        }
        return page.map(IssueResponse::from);
    }

    /** A student's books, newest first: given in the year, or all years when year is null. */
    @Transactional(readOnly = true)
    public List<IssueResponse> studentYear(Long studentId, Integer year) {
        List<BookIssue> list = year == null
                ? issues.findByStudentIdOrderByIssueDateDescIdDesc(studentId)
                : issues.findByStudentIdAndIssueDateBetweenOrderByIssueDateDescIdDesc(studentId, LocalDate.of(year, 1, 1),
                        LocalDate.of(year, 12, 31));
        return list.stream().map(IssueResponse::from).toList();
    }

    /** The books report's student list: every student's totals across all years. */
    @Transactional(readOnly = true)
    public List<org.ngolibrary.dto.StudentSummary> studentSummaries() {
        return issues.summarizeStudents().stream()
                .map(r -> new org.ngolibrary.dto.StudentSummary(r.getStudentId(), r.getStudentName(), r.getTotal(),
                        r.getReturned(), r.getIssued(), r.getOverdue()))
                .toList();
    }

    /** The books report's circles: how many books each student was given in the year. */
    @Transactional(readOnly = true)
    public List<org.ngolibrary.dto.ReaderCount> readersInYear(int year) {
        return issues.countReadersBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).stream()
                .map(r -> new org.ngolibrary.dto.ReaderCount(r.getStudentId(), r.getStudentName(), r.getBooks()))
                .toList();
    }

    /** The book transactions report: every issue given or returned in the month. */
    @Transactional(readOnly = true)
    public List<IssueResponse> movedInMonth(YearMonth month) {
        return issues.findMovedBetween(month.atDay(1), month.atEndOfMonth()).stream().map(IssueResponse::from).toList();
    }

    /** Years that have at least one issue in their range, latest first (current year if there are none). */
    @Transactional(readOnly = true)
    public List<Integer> issueYears() {
        BookIssueRepository.IssueDateRange range = issues.issueDateRange();
        int current = LocalDate.now().getYear();
        int first = range == null || range.getEarliest() == null ? current : range.getEarliest().getYear();
        int last = range == null || range.getLatest() == null ? current : Math.max(range.getLatest().getYear(), first);
        List<Integer> years = new ArrayList<>();
        for (int y = last; y >= first; y--) {
            years.add(y);
        }
        return years;
    }

    @Transactional(readOnly = true)
    public long countByStatus(IssueStatus status, String q) {
        if (q != null && !q.trim().isEmpty()) {
            return issues.countByBookTitleOrStudentName(q.trim());
        }
        if (status == IssueStatus.ISSUED) {
            return issues.countByStatusAndDueDateAfterOrEqual(IssueStatus.ISSUED, LocalDate.now());
        }
        if (status == IssueStatus.OVERDUE) {
            return issues.countByStatusAndDueDateBefore(IssueStatus.ISSUED, LocalDate.now());
        }
        return issues.countByStatus(status);
    }

    /** Tab counts; with a year they count only issues made in that year. */
    @Transactional(readOnly = true)
    public Map<String, Long> counts(String q, Integer year) {
        if (q != null && !q.trim().isEmpty()) {
            long total = issues.countByBookTitleOrStudentName(q.trim());
            return Map.of("all", total, "issued", 0L, "overdue", 0L, "returned", 0L);
        }
        BookIssueRepository.StatusCounts statusCounts = year == null
                ? issues.statusCounts(IssueStatus.ISSUED, IssueStatus.RETURNED, LocalDate.now())
                : issues.statusCountsBetween(IssueStatus.ISSUED, IssueStatus.RETURNED, LocalDate.now(),
                        LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1));
        long issued = statusCounts.getIssued();
        long overdue = statusCounts.getOverdue();
        long returned = statusCounts.getReturned();
        return Map.of(
            "all", issued + overdue + returned,
            "issued", issued,
            "overdue", overdue,
            "returned", returned
        );
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> issuedPaged(Pageable pageable) {
        return issues.findByStatusOrderByIssueDateDesc(IssueStatus.ISSUED, pageable).map(IssueResponse::from);
    }

    /** Every copy still out (overdue included); with a year, only issues made in that year. */
    @Transactional(readOnly = true)
    public Page<IssueResponse> issuedPaged(Pageable pageable, Integer year) {
        if (year == null) {
            return issuedPaged(pageable);
        }
        return issues.findByStatusIssuedBetween(IssueStatus.ISSUED, LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())).map(IssueResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> overduePaged(Pageable pageable) {
        return issues.findByStatusAndDueDateBeforeOrderByIssueDateDesc(IssueStatus.ISSUED, LocalDate.now(), pageable).map(IssueResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> minePaged(UserAccount student, Pageable pageable) {
        return issues.findByStudentOrderByIssueDateDesc(student, pageable).map(IssueResponse::from);
    }

    @Transactional
    public IssueResponse issue(IssueRequest request, UserAccount librarian) {
        Book book = books.findById(request.getBookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        UserAccount student = users.findById(request.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        if (student.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Books can only be issued to students");
        }
        if (!student.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student account is inactive");
        }
        if (issues.existsByStudentAndBookIdAndStatus(student, book.getId(), IssueStatus.ISSUED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student already has this book issued");
        }
        if (books.takeCopy(book.getId()) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No copies available");
        }

        BookIssue issue = new BookIssue();
        issue.setBook(book);
        issue.setStudent(student);
        issue.setIssuedBy(librarian);
        issue.setIssueDate(LocalDate.now());
        issue.setDueDate(LocalDate.now().plusDays(request.getDays() <= 0 ? 14 : request.getDays()));
        issue.setStatus(IssueStatus.ISSUED);
        return IssueResponse.from(issues.save(issue));
    }

    @Transactional
    public IssueResponse returnBook(Long issueId) {
        BookIssue issue = issues.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue record not found"));
        if (issue.getStatus() == IssueStatus.RETURNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book already returned");
        }
        issue.setStatus(IssueStatus.RETURNED);
        issue.setReturnDate(LocalDate.now());
        books.returnCopy(issue.getBook().getId());
        return IssueResponse.from(issues.save(issue));
    }

    /**
     * Dashboard numbers; with a year, the issue numbers (out, overdue) count only issues made in
     * that year. Book, copy and people totals are always for the whole library.
     */
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Integer year) {
        BookRepository.Totals totals = books.totals();
        BookIssueRepository.StatusCounts statusCounts = year == null
                ? issues.statusCounts(IssueStatus.ISSUED, IssueStatus.RETURNED, LocalDate.now())
                : issues.statusCountsBetween(IssueStatus.ISSUED, IssueStatus.RETURNED, LocalDate.now(),
                        LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1));
        long overdue = statusCounts.getOverdue();
        // "Issued" on the dashboard means every copy currently out, overdue included
        long issued = statusCounts.getIssued() + overdue;
        long students = users.countByRole(Role.STUDENT);
        long librarians = users.countByRole(Role.LIBRARIAN);
        return new DashboardResponse(totals.getTitles(), totals.getTotalCopies(), totals.getAvailableCopies(),
                issued, overdue, students, librarians);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getByBookId(Long bookId) {
        return issues.findByBookIdOrderByIssueDateDesc(bookId).stream()
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }
}
