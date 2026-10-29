package org.ngolibrary.service;

import java.util.List;

import org.ngolibrary.domain.Book;
import org.ngolibrary.domain.ReaderRequest;
import org.ngolibrary.domain.ReaderRequest.Kind;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.repo.BookRepository;
import org.ngolibrary.repo.ReaderRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Students ask to borrow a catalog book or suggest one the library lacks; staff see the list and clear it. */
@Service
public class ReaderRequestService {

    private final ReaderRequestRepository requests;
    private final BookRepository books;

    public ReaderRequestService(ReaderRequestRepository requests, BookRepository books) {
        this.requests = requests;
        this.books = books;
    }

    public List<ReaderRequest> list() {
        return requests.findAllByOrderByCreatedAtDesc();
    }

    /** One open request per student and book: asking again only says it is already asked. */
    public ReaderRequest request(Long bookId, UserAccount student) {
        Book book = books.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        if (requests.existsByKindAndStudentIdAndBookId(Kind.REQUEST, student.getId(), bookId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already requested this book.");
        }
        return requests.save(new ReaderRequest(Kind.REQUEST, student, bookId, book.getTitle(), book.getAuthor(), null));
    }

    public ReaderRequest suggest(String title, String author, String note, UserAccount student) {
        String t = trimmed(title, 200);
        if (t == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the book's title.");
        }
        return requests.save(new ReaderRequest(Kind.SUGGESTION, student, null, t, trimmed(author, 200), trimmed(note, 300)));
    }

    /** Dealt with: the book was given, or the suggestion considered. */
    public void clear(Long id) {
        if (!requests.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
        }
        requests.deleteById(id);
    }

    /** Trimmed text, or null when empty; too long is refused rather than cut. */
    private static String trimmed(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keep it under " + max + " characters.");
        }
        return v;
    }
}
