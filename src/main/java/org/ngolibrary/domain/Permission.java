package org.ngolibrary.domain;

/** What a role may do across the app; set per role in the Roles and permissions master. */
public enum Permission {
    READ,
    WRITE,
    UPDATE,
    DELETE,
    GALAXY_VIEW,
    /** Phone a newspaper or magazine vendor from the registers. */
    CALL,
    /** Give a book to a student. */
    ISSUE_BOOK
}
