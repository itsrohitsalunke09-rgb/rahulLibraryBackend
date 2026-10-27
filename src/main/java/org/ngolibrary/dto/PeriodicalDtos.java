package org.ngolibrary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.ngolibrary.domain.PeriodicalFrequency;

/** Request and response shapes for newspapers & magazines and their daily register. */
public final class PeriodicalDtos {

    private PeriodicalDtos() {
    }

    public record PeriodicalRequest(String name, String language, PeriodicalFrequency frequency, BigDecimal price,
                                    Boolean active) {
    }

    /** What the catalog shows anyone signed in: no prices. */
    public record PeriodicalSummary(Long id, String name, String language, PeriodicalFrequency frequency) {
    }

    /** One line of a day's register: whether the copy came and what it cost. */
    public record RegisterRow(Long periodicalId, String name, String language, PeriodicalFrequency frequency,
                              BigDecimal defaultPrice, boolean arrived, BigDecimal price) {
    }

    public record RegisterEntry(Long periodicalId, boolean arrived, BigDecimal price) {
    }

    public record DayRegister(LocalDate date, List<RegisterRow> rows) {
    }

    /**
     * missingDays: days so far this month a daily paper didn't come (null for non-daily).
     * prices: one entry per day of the month (index 0 = the 1st), the price paid or null when no copy came.
     */
    public record ReportRow(Long periodicalId, String name, PeriodicalFrequency frequency, long issues, BigDecimal amount,
                            Long missingDays, List<BigDecimal> prices) {
    }

    public record MonthReport(String month, LocalDate from, LocalDate to, List<ReportRow> rows, long totalIssues,
                              BigDecimal totalAmount) {
    }
}
