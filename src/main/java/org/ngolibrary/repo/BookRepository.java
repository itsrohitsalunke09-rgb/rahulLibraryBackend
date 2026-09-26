package org.ngolibrary.repo;

import java.util.List;

import org.ngolibrary.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT b FROM Book b WHERE lower(b.title) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(b.author) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(b.category) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(b.registrationNumber) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(b.publication) LIKE lower(concat('%', :q, '%'))")
    List<Book> search(@Param("q") String q);

    @Query("SELECT b FROM Book b WHERE lower(b.title) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(b.author) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(b.category) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(b.registrationNumber) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(b.publication) LIKE lower(concat('%', :q, '%'))")
    Page<Book> searchPaged(@Param("q") String q, Pageable pageable);

    Page<Book> findAll(Pageable pageable);
}
