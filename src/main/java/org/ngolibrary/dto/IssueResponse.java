package org.ngolibrary.dto;

import java.time.LocalDate;

import org.ngolibrary.domain.BookIssue;
import org.ngolibrary.domain.IssueStatus;

public class IssueResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String author;
    private Long studentId;
    private String studentName;
    private String issuedByName;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private IssueStatus status;
    private String category;

    public static IssueResponse from(BookIssue issue) {
        IssueResponse response = new IssueResponse();
        response.id = issue.getId();
        response.bookId = issue.getBook().getId();
        response.bookTitle = issue.getBook().getTitle();
        response.author = issue.getBook().getAuthor();
        response.studentId = issue.getStudent().getId();
        response.studentName = issue.getStudent().getFullName();
        response.issuedByName = issue.getIssuedBy().getFullName();
        response.issueDate = issue.getIssueDate();
        response.dueDate = issue.getDueDate();
        response.returnDate = issue.getReturnDate();
        response.status = issue.getStatus();
        response.category = issue.getBook().getCategory();
        if (issue.getStatus() == IssueStatus.ISSUED && issue.getDueDate().isBefore(LocalDate.now())) {
            response.status = IssueStatus.OVERDUE;
        }
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getAuthor() {
        return author;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
