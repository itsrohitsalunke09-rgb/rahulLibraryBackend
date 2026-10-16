package org.ngolibrary.repo;

import java.util.List;

import org.ngolibrary.domain.Book;
import org.ngolibrary.domain.BookCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    long countByCategoryRef(BookCategory category);

    /** Copies a renamed master category's name onto its linked books. */
    @Modifying
    @Query("UPDATE Book b SET b.category = :#{#category.name} WHERE b.categoryRef = :category")
    int syncCategoryName(@Param("category") BookCategory category);

    /** Links books whose old text category has this master category's name (ignoring case). */
    @Modifying
    @Query("UPDATE Book b SET b.categoryRef = :category, b.category = :#{#category.name} "
            + "WHERE b.categoryRef IS NULL AND lower(trim(b.category)) = lower(:#{#category.name})")
    int linkByName(@Param("category") BookCategory category);

    /** Text categories on books that are not linked to the master, with how many books use each. */
    @Query("SELECT b.category AS name, count(b) AS books FROM Book b "
            + "WHERE b.categoryRef IS NULL AND b.category IS NOT NULL AND trim(b.category) <> '' "
            + "GROUP BY b.category ORDER BY b.category")
    List<UnlinkedCategory> unlinkedCategories();

    interface UnlinkedCategory {
        String getName();
        long getBooks();
    }

    @Query("SELECT count(b) as titles, coalesce(sum(b.totalCopies), 0) as totalCopies, "
            + "coalesce(sum(b.availableCopies), 0) as availableCopies FROM Book b")
    Totals totals();

    interface Totals {
        long getTitles();
        long getTotalCopies();
        long getAvailableCopies();
    }
}
