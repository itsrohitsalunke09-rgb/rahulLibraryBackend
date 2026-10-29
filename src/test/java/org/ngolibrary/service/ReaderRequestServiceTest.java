package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.Book;
import org.ngolibrary.domain.ReaderRequest;
import org.ngolibrary.domain.ReaderRequest.Kind;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.repo.BookRepository;
import org.ngolibrary.repo.ReaderRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ReaderRequestServiceTest {

    private ReaderRequestRepository requests;
    private BookRepository books;
    private ReaderRequestService service;
    private UserAccount student;

    @BeforeEach
    void setUp() {
        requests = mock(ReaderRequestRepository.class);
        books = mock(BookRepository.class);
        service = new ReaderRequestService(requests, books);
        when(requests.save(any())).thenAnswer(call -> call.getArgument(0));
        student = new UserAccount();
        student.setId(7L);
        student.setFullName("Asha Patil");
        student.setRole(Role.STUDENT);
    }

    private static HttpStatus status(Runnable call) {
        return assertThrows(ResponseStatusException.class, call::run).getStatus();
    }

    @Test
    void aRequestCopiesTheBookAndIsAskedOnlyOnce() {
        Book book = mock(Book.class);
        when(book.getTitle()).thenReturn("Shyamchi Aai");
        when(book.getAuthor()).thenReturn("Sane Guruji");
        when(books.findById(3L)).thenReturn(Optional.of(book));

        ReaderRequest r = service.request(3L, student);
        assertEquals(Kind.REQUEST, r.getKind());
        assertEquals("Shyamchi Aai", r.getTitle());
        assertEquals("Asha Patil", r.getStudentName());

        when(requests.existsByKindAndStudentIdAndBookIdAndDoneAtIsNull(Kind.REQUEST, 7L, 3L)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, status(() -> service.request(3L, student)));
        assertEquals(HttpStatus.NOT_FOUND, status(() -> service.request(4L, student)));
    }

    @Test
    void doneKeepsTheRecordAndStampsItOnce() {
        ReaderRequest r = service.suggest("Wings of Fire", null, null, student);
        when(requests.findById(5L)).thenReturn(Optional.of(r));
        assertNull(r.getDoneAt());

        service.done(5L);
        java.time.LocalDateTime first = r.getDoneAt();
        assertNotNull(first);
        service.done(5L);
        assertEquals(first, r.getDoneAt());
        verify(requests, never()).deleteById(any());
        assertEquals(HttpStatus.NOT_FOUND, status(() -> service.done(6L)));
    }

    @Test
    void aSuggestionNeedsATitleAndTrimsTheRest() {
        ReaderRequest r = service.suggest("  Wings of Fire ", " ", null, student);
        assertEquals(Kind.SUGGESTION, r.getKind());
        assertEquals("Wings of Fire", r.getTitle());
        assertNull(r.getAuthor());
        assertNull(r.getBookId());

        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.suggest(" ", "x", null, student)));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.suggest("t", null, "n".repeat(301), student)));
    }
}
