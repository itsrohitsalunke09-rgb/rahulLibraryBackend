package org.ngolibrary.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

/** The search queries against a real (in-memory) database, so their JPQL and ordering are checked. */
@DataJpaTest(properties = "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect")
class BookRepositoryTest {

    @Autowired
    private BookRepository books;

    @BeforeEach
    void someBooks() {
        for (String reg : List.of("12", "112", "120", "121", "129", "5")) {
            Book b = new Book();
            b.setTitle("Book " + reg);
            b.setAuthor("Author");
            b.setRegistrationNumber(reg);
            b.setTotalCopies(1);
            b.setAvailableCopies(1);
            books.save(b);
        }
    }

    private static List<String> regs(List<Book> page) {
        return page.stream().map(Book::getRegistrationNumber).toList();
    }

    @Test
    void anExactRegistrationNumberComesFirstThenNewestFirst() {
        var page = books.searchByRegistrationNumberDesc("12", PageRequest.of(0, 20));
        assertEquals(List.of("12", "129", "121", "120", "112"), regs(page.getContent()));
        assertEquals(5, page.getTotalElements());
    }

    @Test
    void theSameInOldestFirstOrder() {
        assertEquals(List.of("12", "112", "120", "121", "129"),
                regs(books.searchByRegistrationNumber("12", PageRequest.of(0, 20)).getContent()));
    }
}
