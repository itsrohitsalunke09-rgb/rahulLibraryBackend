package org.ngolibrary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.ngolibrary.domain.PeriodicalFrequency;

/** Request and response shapes for newspapers & magazines, the daily register and the magazine register. */
public final class PeriodicalDtos {

    private PeriodicalDtos() {
    }

    /**
     * subscription*: magazines only (paid yearly); a missing end is a year after the start.
     * contactNumber: who to phone when a copy doesn't come (optional).
     */
    public record PeriodicalRequest(String name, String language, PeriodicalFrequency frequency, BigDecimal price,
                                    Boolean active, BigDecimal subscriptionAmount, LocalDate subscriptionStart,
                                    LocalDate subscriptionEnd, String contactNumber) {
    }

    /**
     * A magazine issue: its number (अंक) and dates. Monthly: the published date (from = to = that day).
     * Weekly/fortnightly: the from–to dates it covers. receivedOn is the published date or the week's start.
     */
    public record ReceiptRequest(Integer issueNo, LocalDate receivedOn, LocalDate periodFrom, LocalDate periodTo) {
    }

    public record ReceiptView(Long id, Integer issueNo, LocalDate receivedOn, LocalDate periodFrom, LocalDate periodTo) {
    }

    /** One weekly/fortnightly/monthly magazine with its subscription year and every issue received. */
    public record MagazineCard(Long periodicalId, String name, String language, PeriodicalFrequency frequency,
                               BigDecimal subscriptionAmount, LocalDate subscriptionStart, LocalDate subscriptionEnd,
                               List<ReceiptView> receipts, String contactNumber) {
    }

    /** What the catalog shows anyone signed in: no prices. */
    public record PeriodicalSummary(Long id, String name, String language, PeriodicalFrequency frequency) {
    }

    /** One line of a day's register: whether the copy came and what it cost. */
    public record RegisterRow(Long periodicalId, String name, String language, PeriodicalFrequency frequency,
                              BigDecimal defaultPrice, boolean arrived, BigDecimal price, String contactNumber) {
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
