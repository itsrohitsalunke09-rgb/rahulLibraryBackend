package org.ngolibrary.dto;

/** A कात्रण: its title and the years it covers; a missing "to" year means the same year. */
public record ClippingRequest(String title, Integer yearFrom, Integer yearTo) {
}
