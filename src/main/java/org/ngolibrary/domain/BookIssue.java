package org.ngolibrary.domain;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "book_issues", indexes = {
        @Index(name = "idx_book_issues_student", columnList = "student_id"),
        @Index(name = "idx_book_issues_book", columnList = "book_id"),
        @Index(name = "idx_book_issues_status", columnList = "status"),
        @Index(name = "idx_book_issues_due_date", columnList = "dueDate"),
        @Index(name = "idx_book_issues_issued_by", columnList = "issued_by_id")
})
public class BookIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Book book;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private UserAccount student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private UserAccount issuedBy;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status;

    private LocalDate lastOverdueNotificationDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public UserAccount getStudent() {
        return student;
    }

    public void setStudent(UserAccount student) {
        this.student = student;
    }

    public UserAccount getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(UserAccount issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public LocalDate getLastOverdueNotificationDate() {
        return lastOverdueNotificationDate;
    }

    public void setLastOverdueNotificationDate(LocalDate lastOverdueNotificationDate) {
        this.lastOverdueNotificationDate = lastOverdueNotificationDate;
    }
}
