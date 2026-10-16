package org.ngolibrary.service;

import java.util.Comparator;
import java.util.List;

import org.ngolibrary.domain.BookCategory;
import org.ngolibrary.dto.BookCategoryRequest;
import org.ngolibrary.repo.BookCategoryRepository;
import org.ngolibrary.repo.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Book category master. Books keep the category name, so renaming a category
 * renames it on its books too, and a category in use cannot be deleted.
 */
@Service
public class BookCategoryService {

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

    public BookCategory create(BookCategoryRequest request) {
        BookCategory category = new BookCategory();
        apply(category, request);
        return categories.save(category);
    }

    @Transactional
    public BookCategory update(Long id, BookCategoryRequest request) {
        BookCategory category = find(id);
        String oldName = category.getName();
        apply(category, request);
        BookCategory saved = categories.save(category);
        if (!oldName.equals(saved.getName())) {
            books.renameCategory(oldName, saved.getName());
        }
        return saved;
    }

    public void delete(Long id) {
        BookCategory category = find(id);
        long used = books.countByCategoryIgnoreCase(category.getName());
        if (used > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "\"" + category.getName() + "\" is used by " + used + (used == 1 ? " book" : " books")
                            + ". Move those books to another category first.");
        }
        categories.delete(category);
    }

    private BookCategory find(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private void apply(BookCategory category, BookCategoryRequest request) {
        String number = request.getCategoryNumber().trim();
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

    private static Long numericOrNull(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
