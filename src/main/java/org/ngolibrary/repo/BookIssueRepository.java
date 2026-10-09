package org.ngolibrary.repo;

import java.time.LocalDate;
import java.util.List;

import org.ngolibrary.domain.BookIssue;
import org.ngolibrary.domain.IssueStatus;
import org.ngolibrary.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookIssueRepository extends JpaRepository<BookIssue, Long> {
    List<BookIssue> findByStudentOrderByIssueDateDesc(UserAccount student);

    @Query("SELECT bi FROM BookIssue bi WHERE bi.student = :student ORDER BY bi.issueDate DESC")
    Page<BookIssue> findByStudentOrderByIssueDateDesc(@Param("student") UserAccount student, Pageable pageable);
    List<BookIssue> findByStatus(IssueStatus status);
    List<BookIssue> findByStatusAndDueDateBefore(IssueStatus status, LocalDate date);
    long countByStatus(IssueStatus status);
    boolean existsByStudentAndBookIdAndStatus(UserAccount student, Long bookId, IssueStatus status);

    List<BookIssue> findByBookIdOrderByIssueDateDesc(Long bookId);

    long countByBookIdAndStatus(Long bookId, IssueStatus status);

    @Modifying
    @Query("DELETE FROM BookIssue bi WHERE bi.book.id = :bookId")
    void deleteByBookId(@Param("bookId") Long bookId);

    List<BookIssue> findByStatusAndDueDateBetween(IssueStatus status, LocalDate start, LocalDate end);

    long countByStudentIdAndStatus(Long studentId, IssueStatus status);

    @Query("SELECT bi FROM BookIssue bi WHERE bi.status = :status ORDER BY bi.issueDate DESC")
    Page<BookIssue> findByStatusOrderByIssueDateDesc(@Param("status") IssueStatus status, Pageable pageable);

    @Query("SELECT bi FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate < :date ORDER BY bi.issueDate DESC")
    Page<BookIssue> findByStatusAndDueDateBeforeOrderByIssueDateDesc(@Param("status") IssueStatus status, @Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT bi FROM BookIssue bi WHERE bi.status = :status ORDER BY bi.issueDate DESC")
    Page<BookIssue> findByStatusPaged(@Param("status") IssueStatus status, Pageable pageable);

    @Query("SELECT bi FROM BookIssue bi WHERE (lower(bi.book.title) LIKE lower(concat('%', :q, '%')) OR lower(bi.student.fullName) LIKE lower(concat('%', :q, '%'))) ORDER BY bi.issueDate DESC")
    Page<BookIssue> searchByBookTitleOrStudentName(@Param("q") String q, Pageable pageable);

    @Query("SELECT count(bi) FROM BookIssue bi WHERE (lower(bi.book.title) LIKE lower(concat('%', :q, '%')) OR lower(bi.student.fullName) LIKE lower(concat('%', :q, '%')))")
    long countByBookTitleOrStudentName(@Param("q") String q);

    @Query("SELECT count(bi) FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate < :date")
    long countByStatusAndDueDateBefore(@Param("status") IssueStatus status, @Param("date") LocalDate date);

    @Query("SELECT count(bi) FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate >= :date")
    long countByStatusAndDueDateAfterOrEqual(@Param("status") IssueStatus status, @Param("date") LocalDate date);

    @Query("SELECT bi FROM BookIssue bi WHERE bi.status = :status AND bi.dueDate >= :date ORDER BY bi.issueDate DESC")
    Page<BookIssue> findByStatusAndDueDateAfterOrEqual(@Param("status") IssueStatus status, @Param("date") LocalDate date, Pageable pageable);
}
