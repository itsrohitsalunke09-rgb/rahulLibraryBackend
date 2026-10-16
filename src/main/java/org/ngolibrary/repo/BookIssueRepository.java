package org.ngolibrary.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.ngolibrary.domain.BookIssue;
import org.ngolibrary.domain.IssueStatus;
import org.ngolibrary.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookIssueRepository extends JpaRepository<BookIssue, Long> {

    // Book, student and issuer are read for every IssueResponse; fetch them in the same query
    // instead of one extra round trip per row.
    @Override
    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    Page<BookIssue> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    Optional<BookIssue> findById(Long id);

    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    List<BookIssue> findByStudentOrderByIssueDateDesc(UserAccount student);

    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    @Query(value = "SELECT bi FROM BookIssue bi WHERE bi.student = :student ORDER BY bi.issueDate DESC",
            countQuery = "SELECT count(bi) FROM BookIssue bi WHERE bi.student = :student")
    Page<BookIssue> findByStudentOrderByIssueDateDesc(@Param("student") UserAccount student, Pageable pageable);

    List<BookIssue> findByStatus(IssueStatus status);

    @EntityGraph(attributePaths = {"book", "student"})
    List<BookIssue> findByStatusAndDueDateBefore(IssueStatus status, LocalDate date);

    long countByStatus(IssueStatus status);
    boolean existsByStudentAndBookIdAndStatus(UserAccount student, Long bookId, IssueStatus status);

    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    List<BookIssue> findByBookIdOrderByIssueDateDesc(Long bookId);

    long countByBookIdAndStatus(Long bookId, IssueStatus status);

    @Modifying
    @Query("DELETE FROM BookIssue bi WHERE bi.book.id = :bookId")
    void deleteByBookId(@Param("bookId") Long bookId);

    @EntityGraph(attributePaths = {"book", "student"})
    List<BookIssue> findByStatusAndDueDateBetween(IssueStatus status, LocalDate start, LocalDate end);

    long countByStudentIdAndStatus(Long studentId, IssueStatus status);

    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    @Query(value = "SELECT bi FROM BookIssue bi WHERE bi.status = :status ORDER BY bi.issueDate DESC",
            countQuery = "SELECT count(bi) FROM BookIssue bi WHERE bi.status = :status")
    Page<BookIssue> findByStatusOrderByIssueDateDesc(@Param("status") IssueStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    @Query(value = "SELECT bi FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate < :date ORDER BY bi.issueDate DESC",
            countQuery = "SELECT count(bi) FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate < :date")
    Page<BookIssue> findByStatusAndDueDateBeforeOrderByIssueDateDesc(@Param("status") IssueStatus status, @Param("date") LocalDate date, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    @Query(value = "SELECT bi FROM BookIssue bi WHERE bi.status = :status ORDER BY bi.issueDate DESC",
            countQuery = "SELECT count(bi) FROM BookIssue bi WHERE bi.status = :status")
    Page<BookIssue> findByStatusPaged(@Param("status") IssueStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    @Query(value = "SELECT bi FROM BookIssue bi WHERE (lower(bi.book.title) LIKE lower(concat('%', :q, '%')) OR lower(bi.student.fullName) LIKE lower(concat('%', :q, '%'))) ORDER BY bi.issueDate DESC",
            countQuery = "SELECT count(bi) FROM BookIssue bi WHERE (lower(bi.book.title) LIKE lower(concat('%', :q, '%')) OR lower(bi.student.fullName) LIKE lower(concat('%', :q, '%')))")
    Page<BookIssue> searchByBookTitleOrStudentName(@Param("q") String q, Pageable pageable);

    @Query("SELECT count(bi) FROM BookIssue bi WHERE (lower(bi.book.title) LIKE lower(concat('%', :q, '%')) OR lower(bi.student.fullName) LIKE lower(concat('%', :q, '%')))")
    long countByBookTitleOrStudentName(@Param("q") String q);

    /** Issues whose issue date falls in [from, to), newest first (used for the year filter). */
    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    @Query(value = "SELECT bi FROM BookIssue bi WHERE bi.issueDate >= :from AND bi.issueDate < :to ORDER BY bi.issueDate DESC, bi.id DESC",
            countQuery = "SELECT count(bi) FROM BookIssue bi WHERE bi.issueDate >= :from AND bi.issueDate < :to")
    Page<BookIssue> findIssuedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    /** Earliest and latest issue dates, to offer the years that have issues. */
    @Query("SELECT min(bi.issueDate) AS earliest, max(bi.issueDate) AS latest FROM BookIssue bi")
    IssueDateRange issueDateRange();

    interface IssueDateRange {
        LocalDate getEarliest();
        LocalDate getLatest();
    }

    @Query("SELECT count(bi) FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate < :date")
    long countByStatusAndDueDateBefore(@Param("status") IssueStatus status, @Param("date") LocalDate date);

    @Query("SELECT count(bi) FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate >= :date")
    long countByStatusAndDueDateAfterOrEqual(@Param("status") IssueStatus status, @Param("date") LocalDate date);

    @EntityGraph(attributePaths = {"book", "student", "issuedBy"})
    @Query(value = "SELECT bi FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate >= :date ORDER BY bi.issueDate DESC",
            countQuery = "SELECT count(bi) FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate >= :date")
    Page<BookIssue> findByStatusAndDueDateAfterOrEqual(@Param("status") IssueStatus status, @Param("date") LocalDate date, Pageable pageable);

    /** Issued (not yet due), overdue and returned counts in a single query. */
    @Query("SELECT "
            + "coalesce(sum(case when bi.status = :issued and bi.dueDate >= :today then 1 else 0 end), 0) as issued, "
            + "coalesce(sum(case when bi.status = :issued and bi.dueDate < :today then 1 else 0 end), 0) as overdue, "
            + "coalesce(sum(case when bi.status = :returned then 1 else 0 end), 0) as returned "
            + "FROM BookIssue bi")
    StatusCounts statusCounts(@Param("issued") IssueStatus issued, @Param("returned") IssueStatus returned,
                              @Param("today") LocalDate today);

    interface StatusCounts {
        long getIssued();
        long getOverdue();
        long getReturned();
    }
}
