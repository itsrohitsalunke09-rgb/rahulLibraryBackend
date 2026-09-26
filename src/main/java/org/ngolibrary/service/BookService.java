package org.ngolibrary.service;

import java.util.List;

import org.ngolibrary.domain.Book;
import org.ngolibrary.domain.BookIssue;
import org.ngolibrary.domain.IssueStatus;
import org.ngolibrary.dto.BookRequest;
import org.ngolibrary.repo.BookIssueRepository;
import org.ngolibrary.repo.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

    private final BookRepository books;
    private final BookIssueRepository issues;

    public BookService(BookRepository books, BookIssueRepository issues) {
        this.books = books;
        this.issues = issues;
    }

    public List<Book> list(String query) {
        if (query == null || query.isBlank()) {
            return books.findAll();
        }
        return books.search(query.trim());
    }

    public Page<Book> listPaged(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return books.findAll(pageable);
        }
        return books.searchPaged(query.trim(), pageable);
    }

    public Book create(BookRequest request) {
        Book book = new Book();
        apply(book, request, true);
        return books.save(book);
    }

    public Book update(Long id, BookRequest request) {
        Book book = books.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        apply(book, request, false);
        return books.save(book);
    }

    public Book getById(Long id) {
        return books.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    @Transactional
    public void delete(Long id) {
        if (!books.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }
        // Delete all associated issues first (cascade delete)
        issues.deleteByBookId(id);
        books.deleteById(id);
    }

    private void apply(Book book, BookRequest request, boolean creating) {
        int oldTotal = book.getTotalCopies();
        int oldAvailable = book.getAvailableCopies();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategory(request.getCategory());
        book.setDescription(request.getDescription());
        book.setDate(request.getDate());
        book.setRegistrationNumber(request.getRegistrationNumber());
        book.setPublication(request.getPublication());
        book.setPublicationYear(request.getPublicationYear());
        book.setPageCount(request.getPageCount());
        book.setSeller(request.getSeller());
        book.setReceiptNumber(request.getReceiptNumber());
        book.setPrice(request.getPrice());
        book.setTotalCopies(request.getTotalCopies());
        if (creating) {
            book.setAvailableCopies(request.getTotalCopies());
        } else {
            int issued = oldTotal - oldAvailable;
            int available = request.getTotalCopies() - issued;
            if (available < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Total copies cannot be less than currently issued copies (" + issued + ")");
            }
            book.setAvailableCopies(available);
        }
    }
}