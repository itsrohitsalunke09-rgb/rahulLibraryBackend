package org.ngolibrary.dto;

/** A student's books across all years: total, and how many are returned, still issued, or overdue. */
public record StudentSummary(Long studentId, String studentName, Long total, Long returned, Long issued, Long overdue) {
}
