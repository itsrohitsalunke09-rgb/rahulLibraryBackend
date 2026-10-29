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
    /** Issue books to students, and mark newspapers and magazines received in the registers. */
    REGISTER
}
