package org.ngolibrary.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A student's request to borrow a catalog book, or suggestion of a book the library doesn't have.
 * Names and titles are copied in, so the list reads the same after a book or account changes.
 * Staff clear it once dealt with.
 */
@Entity
@Table(name = "reader_requests")
public class ReaderRequest {

    public enum Kind { REQUEST, SUGGESTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String studentName;

    /** The catalog book asked for; empty for a suggestion. */
    private Long bookId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String author;

    @Column(length = 300)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ReaderRequest() {
    }

    public ReaderRequest(Kind kind, UserAccount student, Long bookId, String title, String author, String note) {
        this.kind = kind;
        this.studentId = student.getId();
        this.studentName = student.getFullName();
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Kind getKind() {
        return kind;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
