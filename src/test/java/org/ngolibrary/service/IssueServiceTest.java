package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.Book;
import org.ngolibrary.domain.BookIssue;
import org.ngolibrary.domain.IssueStatus;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.IssueRequest;
import org.ngolibrary.dto.IssueResponse;
import org.ngolibrary.repo.BookIssueRepository;
import org.ngolibrary.repo.BookRepository;
import org.ngolibrary.repo.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class IssueServiceTest {

    private BookIssueRepository issues;
    private BookRepository books;
    private UserAccountRepository users;
    private IssueService service;
    private Book book;
    private UserAccount student;
    private UserAccount librarian;

    @BeforeEach
    void setUp() {
        issues = mock(BookIssueRepository.class);
        books = mock(BookRepository.class);
        users = mock(UserAccountRepository.class);
        service = new IssueService(issues, books, users);
        when(issues.save(any(BookIssue.class))).thenAnswer(inv -> inv.getArgument(0));

        book = new Book();
        book.setId(1L);
        book.setTitle("Yayati");
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
        student = account(2L, Role.STUDENT);
        librarian = account(3L, Role.LIBRARIAN);
        when(books.findById(1L)).thenReturn(Optional.of(book));
        when(users.findById(2L)).thenReturn(Optional.of(student));
    }

    private static UserAccount account(Long id, Role role) {
        UserAccount u = new UserAccount();
        u.setId(id);
        u.setUsername("u" + id);
        u.setFullName("User " + id);
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    private static IssueRequest request(int days) {
        IssueRequest r = new IssueRequest();
        r.setBookId(1L);
        r.setStudentId(2L);
        r.setDays(days);
        return r;
    }

    private static void assertBadRequest(String message, Runnable call) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, call::run);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals(message, e.getReason());
    }

    @Test
    void issueTakesACopyAndSetsTheDueDate() {
        when(books.takeCopy(1L)).thenReturn(1);
        service.issue(request(7), librarian);
        verify(books).takeCopy(1L);
        verify(issues).save(any(BookIssue.class));
    }

    @Test
    void issueFailsWhenTheLastCopyWasJustTaken() {
        when(books.takeCopy(1L)).thenReturn(0);
        assertBadRequest("No copies available", () -> service.issue(request(7), librarian));
        verify(issues, never()).save(any());
    }

    @Test
    void issueOnlyToActiveStudents() {
        student.setRole(Role.LIBRARIAN);
        assertBadRequest("Books can only be issued to students", () -> service.issue(request(7), librarian));
        student.setRole(Role.STUDENT);
        student.setActive(false);
        assertBadRequest("Student account is inactive", () -> service.issue(request(7), librarian));
        verify(books, never()).takeCopy(any());
    }

    @Test
    void returnPutsTheCopyBackOnce() {
        BookIssue issue = new BookIssue();
        issue.setId(9L);
        issue.setBook(book);
        issue.setStudent(student);
        issue.setIssuedBy(librarian);
        issue.setIssueDate(LocalDate.now().minusDays(3));
        issue.setDueDate(LocalDate.now().plusDays(4));
        issue.setStatus(IssueStatus.ISSUED);
        when(issues.findById(9L)).thenReturn(Optional.of(issue));

        service.returnBook(9L);
        assertEquals(IssueStatus.RETURNED, issue.getStatus());
        verify(books).returnCopy(1L);

        assertBadRequest("Book already returned", () -> service.returnBook(9L));
    }

    @Test
    void theMonthReportAsksForIssuesGivenOrReturnedInThatMonth() {
        BookIssue issue = new BookIssue();
        issue.setBook(book);
        issue.setStudent(student);
        issue.setIssuedBy(librarian);
        issue.setIssueDate(LocalDate.of(2026, 10, 3));
        issue.setDueDate(LocalDate.of(2026, 10, 17));
        issue.setStatus(IssueStatus.RETURNED);
        issue.setReturnDate(LocalDate.of(2026, 10, 9));
        when(issues.findMovedBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).thenReturn(List.of(issue));
        List<IssueResponse> moved = service.movedInMonth(java.time.YearMonth.of(2026, 10));
        assertEquals(1, moved.size());
        assertEquals("Yayati", moved.get(0).getBookTitle());
    }

    private static BookIssueRepository.ReaderCountRow row(Long id, String name, Long books) {
        return new BookIssueRepository.ReaderCountRow() {
            public Long getStudentId() { return id; }
            public String getStudentName() { return name; }
            public Long getBooks() { return books; }
        };
    }

    @Test
    void theReadersCountCoversTheWholeYear() {
        when(issues.countReadersBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(row(7L, "Kunal", 23L)));
        assertEquals(23L, service.readersInYear(2026).get(0).books());
    }

    @Test
    void aReadingProfileAsksForTheStudentsBooksInThatYear() {
        BookIssue issue = new BookIssue();
        issue.setBook(book);
        issue.setStudent(student);
        issue.setIssuedBy(librarian);
        issue.setIssueDate(LocalDate.of(2026, 3, 3));
        issue.setDueDate(LocalDate.of(2026, 3, 17));
        issue.setStatus(IssueStatus.RETURNED);
        when(issues.findByStudentIdAndIssueDateBetweenOrderByIssueDateDescIdDesc(2L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(issue));
        assertEquals("Yayati", service.studentYear(2L, 2026).get(0).getBookTitle());
    }

    @Test
    void withoutAYearAStudentsBooksCoverAllYears() {
        BookIssue issue = new BookIssue();
        issue.setBook(book);
        issue.setStudent(student);
        issue.setIssuedBy(librarian);
        issue.setIssueDate(LocalDate.of(2025, 3, 3));
        issue.setDueDate(LocalDate.of(2025, 3, 17));
        issue.setStatus(IssueStatus.RETURNED);
        when(issues.findByStudentIdOrderByIssueDateDescIdDesc(2L)).thenReturn(List.of(issue));
        assertEquals(1, service.studentYear(2L, null).size());
    }

    @Test
    void theStudentSummaryCarriesTheCountsByStatus() {
        when(issues.summarizeStudents()).thenReturn(List.of(new BookIssueRepository.StudentSummaryRow() {
            public Long getStudentId() { return 7L; }
            public String getStudentName() { return "Aarav"; }
            public Long getTotal() { return 9L; }
            public Long getReturned() { return 2L; }
            public Long getIssued() { return 6L; }
            public Long getOverdue() { return 1L; }
        }));
        org.ngolibrary.dto.StudentSummary s = service.studentSummaries().get(0);
        assertEquals(9L, s.total());
        assertEquals(1L, s.overdue());
    }
}
