package org.ngolibrary.repo;

import java.util.Optional;

import org.ngolibrary.domain.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {
    Optional<BookCategory> findByCategoryNumberIgnoreCase(String categoryNumber);

    Optional<BookCategory> findByNameIgnoreCase(String name);
}
