package org.ngolibrary.dto;

/** A student and how many books they were given in a period (the books report's circles). */
public record ReaderCount(Long studentId, String studentName, Long books) {
}
