package org.ngolibrary.web;

import java.util.List;

import javax.validation.Valid;

import org.ngolibrary.domain.BookCategory;
import org.ngolibrary.dto.BookCategoryRequest;
import org.ngolibrary.service.BookCategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuration master: book categories. Anyone signed in can read; staff manage. */
@RestController
@RequestMapping("/api/categories")
public class BookCategoryController {

    private final BookCategoryService categoryService;

    public BookCategoryController(BookCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<BookCategory> list() {
        return categoryService.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public BookCategory create(@Valid @RequestBody BookCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public BookCategory update(@PathVariable Long id, @Valid @RequestBody BookCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
