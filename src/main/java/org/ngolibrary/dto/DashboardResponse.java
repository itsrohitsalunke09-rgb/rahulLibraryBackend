package org.ngolibrary.dto;

public class DashboardResponse {
    private long totalBooks;
    private long totalCopies;
    private long availableCopies;
    private long issuedCopies;
    private long overdueCopies;
    private long students;
    private long librarians;

    public DashboardResponse(long totalBooks, long totalCopies, long availableCopies, long issuedCopies,
                              long overdueCopies, long students, long librarians) {
        this.totalBooks = totalBooks;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.issuedCopies = issuedCopies;
        this.overdueCopies = overdueCopies;
        this.students = students;
        this.librarians = librarians;
    }

    public long getTotalBooks() {
        return totalBooks;
    }

    public long getTotalCopies() {
        return totalCopies;
    }

    public long getAvailableCopies() {
        return availableCopies;
    }

    public long getIssuedCopies() {
        return issuedCopies;
    }

    public long getOverdueCopies() {
        return overdueCopies;
    }

    public long getStudents() {
        return students;
    }

    public long getLibrarians() {
        return librarians;
    }
}
