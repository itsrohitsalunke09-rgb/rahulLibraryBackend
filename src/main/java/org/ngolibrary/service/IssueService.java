package org.ngolibrary.service;

import java.time.LocalDate;
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
    public List<IssueResponse> all() {
        return issues.findAll().stream().map(IssueResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> allPaged(Pageable pageable) {
        return issues.findAll(pageable).map(IssueResponse::from);
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

    @Transactional(readOnly = true)
    public long countByStatus(IssueStatus status) {
        if (status == IssueStatus.ISSUED) {
            return issues.countByStatusAndDueDateAfterOrEqual(IssueStatus.ISSUED, LocalDate.now());
        }
        if (status == IssueStatus.OVERDUE) {
            return issues.countByStatusAndDueDateBefore(IssueStatus.ISSUED, LocalDate.now());
        }
        return issues.countByStatus(status);
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

    @Transactional(readOnly = true)
    public Map<String, Long> counts(String q) {
        if (q != null && !q.trim().isEmpty()) {
            long total = issues.countByBookTitleOrStudentName(q.trim());
            return Map.of("all", total, "issued", 0L, "overdue", 0L, "returned", 0L);
        }
        long issued = issues.countByStatusAndDueDateAfterOrEqual(IssueStatus.ISSUED, LocalDate.now());
        long overdue = issues.countByStatusAndDueDateBefore(IssueStatus.ISSUED, LocalDate.now());
        long returned = issues.countByStatus(IssueStatus.RETURNED);
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

    @Transactional(readOnly = true)
    public Page<IssueResponse> overduePaged(Pageable pageable) {
        return issues.findByStatusAndDueDateBeforeOrderByIssueDateDesc(IssueStatus.ISSUED, LocalDate.now(), pageable).map(IssueResponse::from);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> mine(UserAccount student) {
        return issues.findByStudentOrderByIssueDateDesc(student).stream()
                .map(IssueResponse::from)
                .collect(Collectors.toList());
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
        if (book.getAvailableCopies() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No copies available");
        }
        if (issues.existsByStudentAndBookIdAndStatus(student, book.getId(), IssueStatus.ISSUED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student already has this book issued");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        books.save(book);

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
        Book book = issue.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        books.save(book);
        return IssueResponse.from(issues.save(issue));
    }

    public DashboardResponse dashboard() {
        List<Book> allBooks = books.findAll();
        long titles = allBooks.size();
        long totalCopies = allBooks.stream().mapToLong(Book::getTotalCopies).sum();
        long available = allBooks.stream().mapToLong(Book::getAvailableCopies).sum();
        long issued = issues.countByStatus(IssueStatus.ISSUED);
        long overdue = issues.findByStatusAndDueDateBefore(IssueStatus.ISSUED, LocalDate.now()).size();
        long students = users.findByRole(Role.STUDENT).size();
        long librarians = users.findByRole(Role.LIBRARIAN).size();
        return new DashboardResponse(titles, totalCopies, available, issued, overdue, students, librarians);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getByBookId(Long bookId) {
        return issues.findByBookIdOrderByIssueDateDesc(bookId).stream()
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }
}
