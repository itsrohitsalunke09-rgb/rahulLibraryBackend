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
    private final BookCategoryService categories;

    public BookService(BookRepository books, BookIssueRepository issues, BookCategoryService categories) {
        this.books = books;
        this.issues = issues;
        this.categories = categories;
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

    /** Books in registration-number order (1, 2, 10…, or 10, 2, 1… when descending), books without one last. */
    public Page<Book> listPagedByRegistrationNumber(String query, Pageable pageable, boolean descending) {
        if (query == null || query.isBlank()) {
            return descending ? books.findAllByRegistrationNumberDesc(pageable) : books.findAllByRegistrationNumber(pageable);
        }
        return descending ? books.searchByRegistrationNumberDesc(query.trim(), pageable)
                : books.searchByRegistrationNumber(query.trim(), pageable);
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
        // Keep track of copies still with students: those loans must be returned first.
        long out = issues.countByBookIdAndStatus(id, IssueStatus.ISSUED);
        if (out > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete this book because " + out + " cop" + (out == 1 ? "y is" : "ies are")
                            + " still issued. Please mark them returned first.");
        }
        // Only returned loans are left; they go with the book.
        issues.deleteByBookId(id);
        books.deleteById(id);
    }

    /**
     * Links the book to the master category given by id; otherwise the category text is
     * linked when a master category has that name (the website still sends text) and kept
     * as plain text when none does.
     */
    private void applyCategory(Book book, BookRequest request) {
        if (request.getCategoryId() != null) {
            book.setCategoryRef(categories.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found")));
            return;
        }
        String text = request.getCategory() == null ? null : request.getCategory().trim();
        if (text == null || text.isEmpty()) {
            book.setCategoryRef(null);
            book.setCategory(null);
            return;
        }
        book.setCategoryRef(categories.findByName(text).orElse(null));
        if (book.getCategoryRef() == null) {
            book.setCategory(text);
        }
    }

    /**
     * Every book is one physical book (a second copy is entered as its own book with its own registration
     * number), so a new book is one copy, available. Description is no longer entered; old ones are kept.
     */
    private void apply(Book book, BookRequest request, boolean creating) {
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        applyCategory(book, request);
        book.setDate(request.getDate());
        book.setRegistrationNumber(request.getRegistrationNumber());
        book.setPublication(request.getPublication());
        book.setPublicationYear(request.getPublicationYear());
        book.setPageCount(request.getPageCount());
        book.setSeller(request.getSeller());
        book.setReceiptNumber(request.getReceiptNumber());
        book.setPrice(request.getPrice());
        if (creating) {
            book.setTotalCopies(1);
            book.setAvailableCopies(1);
        }
    }
}