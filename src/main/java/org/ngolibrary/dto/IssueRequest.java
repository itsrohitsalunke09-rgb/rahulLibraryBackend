package org.ngolibrary.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class IssueRequest {
    @NotNull
    private Long bookId;
    @NotNull
    private Long studentId;
    @Min(1)
    private int days = 14;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }
}
