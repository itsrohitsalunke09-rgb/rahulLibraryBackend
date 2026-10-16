package org.ngolibrary.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.ngolibrary.domain.BookCategory;
import org.ngolibrary.dto.BookCategoryRequest;
import org.ngolibrary.repo.BookCategoryRepository;
import org.ngolibrary.repo.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Book category master. Books link to a category and show its name; renaming a
 * category updates its books, and a category in use cannot be deleted. Books that
 * still carry an old text category are linked as soon as a category with that name exists.
 */
@Service
public class BookCategoryService {

    private static final Logger log = LoggerFactory.getLogger(BookCategoryService.class);

    /** Numbers sort numerically when they are numbers ("2" before "10"), otherwise as text. */
    private static final Comparator<BookCategory> BY_NUMBER = Comparator
            .comparing((BookCategory c) -> numericOrNull(c.getCategoryNumber()),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(c -> c.getCategoryNumber().toLowerCase());

    private final BookCategoryRepository categories;
    private final BookRepository books;

    public BookCategoryService(BookCategoryRepository categories, BookRepository books) {
        this.categories = categories;
        this.books = books;
    }

    public List<BookCategory> list() {
        List<BookCategory> all = categories.findAll();
        all.sort(BY_NUMBER);
        return all;
    }

    public Optional<BookCategory> findById(Long id) {
        return categories.findById(id);
    }

    /** Looks up a category by number; "456.0" finds 456 and "456.830" finds 456.83. */
    public Optional<BookCategory> findByNumber(String number) {
        String normalized = normalizeNumber(number);
        return normalized == null ? Optional.empty() : categories.findByCategoryNumberIgnoreCase(normalized);
    }

    public Optional<BookCategory> findByName(String name) {
        return name == null || name.isBlank() ? Optional.empty() : categories.findByNameIgnoreCase(name.trim());
    }

    /** Text categories on books that are not in the master yet, with their book counts. */
    public List<BookRepository.UnlinkedCategory> unlinked() {
        return books.unlinkedCategories();
    }

    @Transactional
    public BookCategory create(BookCategoryRequest request) {
        BookCategory category = new BookCategory();
        apply(category, request);
        BookCategory saved = categories.save(category);
        books.linkByName(saved);
        return saved;
    }

    @Transactional
    public BookCategory update(Long id, BookCategoryRequest request) {
        BookCategory category = find(id);
        apply(category, request);
        BookCategory saved = categories.save(category);
        books.syncCategoryName(saved);
        books.linkByName(saved);
        return saved;
    }

    public void delete(Long id) {
        BookCategory category = find(id);
        long used = books.countByCategoryRef(category);
        if (used > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "\"" + category.getName() + "\" is used by " + used + (used == 1 ? " book" : " books")
                            + ". Move those books to another category first.");
        }
        categories.delete(category);
    }

    /** On start-up, link books whose text category matches a master category's name. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void linkExistingBooks() {
        int linked = 0;
        for (BookCategory category : categories.findAll()) {
            linked += books.linkByName(category);
        }
        if (linked > 0) {
            log.info("Linked {} book(s) to the category master by name", linked);
        }
    }

    /**
     * Category numbers as stored: Excel's 456.0 becomes "456", 456.83 stays "456.83".
     * Values that are not numbers are kept as typed (trimmed). Returns null for blank.
     */
    public static String normalizeNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            BigDecimal number = new BigDecimal(trimmed).stripTrailingZeros();
            return number.scale() < 0 ? number.setScale(0).toPlainString() : number.toPlainString();
        } catch (NumberFormatException e) {
            return trimmed;
        }
    }

    private BookCategory find(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private void apply(BookCategory category, BookCategoryRequest request) {
        String number = normalizeNumber(request.getCategoryNumber());
        String name = request.getName().trim();
        categories.findByCategoryNumberIgnoreCase(number)
                .filter(other -> !other.getId().equals(category.getId()))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Category number " + number + " is already used by \"" + other.getName() + "\"");
                });
        categories.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(category.getId()))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A category named \"" + other.getName() + "\" already exists");
                });
        category.setCategoryNumber(number);
        category.setName(name);
    }

    private static BigDecimal numericOrNull(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
