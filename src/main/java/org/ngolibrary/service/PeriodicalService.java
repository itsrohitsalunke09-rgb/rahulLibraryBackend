package org.ngolibrary.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.ngolibrary.domain.Periodical;
import org.ngolibrary.domain.PeriodicalArrival;
import org.ngolibrary.domain.PeriodicalFrequency;
import org.ngolibrary.dto.PeriodicalDtos.DayRegister;
import org.ngolibrary.dto.PeriodicalDtos.MonthReport;
import org.ngolibrary.dto.PeriodicalDtos.PeriodicalRequest;
import org.ngolibrary.dto.PeriodicalDtos.PeriodicalSummary;
import org.ngolibrary.dto.PeriodicalDtos.RegisterEntry;
import org.ngolibrary.dto.PeriodicalDtos.RegisterRow;
import org.ngolibrary.dto.PeriodicalDtos.ReportRow;
import org.ngolibrary.repo.PeriodicalArrivalRepository;
import org.ngolibrary.repo.PeriodicalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Newspapers & magazines, the daily arrival register, and the monthly report with costs. */
@Service
public class PeriodicalService {

    static final BigDecimal MAX_PRICE = new BigDecimal("10000");

    private final PeriodicalRepository periodicals;
    private final PeriodicalArrivalRepository arrivals;

    public PeriodicalService(PeriodicalRepository periodicals, PeriodicalArrivalRepository arrivals) {
        this.periodicals = periodicals;
        this.arrivals = arrivals;
    }

    public List<Periodical> list() {
        return periodicals.findAllByOrderByActiveDescNameAsc();
    }

    /** The subscribed ones, by name. */
    public List<PeriodicalSummary> subscribed() {
        return list().stream().filter(Periodical::isActive)
                .map(p -> new PeriodicalSummary(p.getId(), p.getName(), p.getLanguage(), p.getFrequency()))
                .toList();
    }

    public Periodical create(PeriodicalRequest request) {
        Periodical p = new Periodical();
        apply(p, request);
        return periodicals.save(p);
    }

    public Periodical update(Long id, PeriodicalRequest request) {
        Periodical p = find(id);
        apply(p, request);
        return periodicals.save(p);
    }

    /** Only one with no register entries can be deleted; otherwise switch it off to keep the history. */
    public void delete(Long id) {
        find(id);
        if (arrivals.existsByPeriodicalId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This one has register entries. Switch off \"Subscribed\" instead, so its history stays.");
        }
        periodicals.deleteById(id);
    }

    /** The day's register: every subscribed newspaper/magazine, plus any old one that has an entry that day. */
    @Transactional(readOnly = true)
    public DayRegister register(LocalDate date) {
        Map<Long, PeriodicalArrival> entries = arrivals.findByDate(date).stream()
                .collect(Collectors.toMap(a -> a.getPeriodical().getId(), Function.identity()));
        List<RegisterRow> rows = new ArrayList<>();
        for (Periodical p : list()) {
            PeriodicalArrival entry = entries.get(p.getId());
            if (p.isActive() || entry != null) {
                rows.add(new RegisterRow(p.getId(), p.getName(), p.getLanguage(), p.getFrequency(), p.getPrice(),
                        entry != null, entry != null ? entry.getPrice() : p.getPrice()));
            }
        }
        return new DayRegister(date, rows);
    }

    /** Records the day: an arrived copy is added or its price updated; an unticked one is removed. */
    @Transactional
    public DayRegister saveRegister(LocalDate date, List<RegisterEntry> entries) {
        if (date.isAfter(LocalDate.now())) {
            throw bad("The register can't be filled in for a future date.");
        }
        Map<Long, PeriodicalArrival> existing = arrivals.findByDate(date).stream()
                .collect(Collectors.toMap(a -> a.getPeriodical().getId(), Function.identity()));
        for (RegisterEntry e : entries == null ? List.<RegisterEntry>of() : entries) {
            PeriodicalArrival entry = existing.get(e.periodicalId());
            if (!e.arrived()) {
                if (entry != null) {
                    arrivals.delete(entry);
                }
                continue;
            }
            Periodical p = find(e.periodicalId());
            if (entry == null) {
                entry = new PeriodicalArrival();
                entry.setPeriodical(p);
                entry.setDate(date);
            }
            entry.setPrice(price(e.price() == null ? p.getPrice() : e.price()));
            arrivals.save(entry);
        }
        return register(date);
    }

    /**
     * Copies and cost per newspaper/magazine for a month. For daily papers it also counts the days
     * (up to today) with no copy, so gaps show up when checking the vendor's bill.
     */
    @Transactional(readOnly = true)
    public MonthReport report(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        LocalDate today = LocalDate.now();
        LocalDate countUntil = to.isAfter(today) ? today : to;
        long daysSoFar = countUntil.isBefore(from) ? 0 : ChronoUnit.DAYS.between(from, countUntil) + 1;

        // Price paid per paper per day of the month (null = no copy that day).
        Map<Long, BigDecimal[]> byDay = new java.util.HashMap<>();
        for (PeriodicalArrival a : arrivals.findByDateBetween(from, to)) {
            byDay.computeIfAbsent(a.getPeriodical().getId(), id -> new BigDecimal[month.lengthOfMonth()])
                    [a.getDate().getDayOfMonth() - 1] = a.getPrice();
        }
        List<ReportRow> rows = new ArrayList<>();
        long totalIssues = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Periodical p : list()) {
            BigDecimal[] days = byDay.get(p.getId());
            if (!p.isActive() && days == null) {
                continue;
            }
            List<BigDecimal> prices = java.util.Arrays.asList(days == null ? new BigDecimal[month.lengthOfMonth()] : days);
            long issues = prices.stream().filter(java.util.Objects::nonNull).count();
            BigDecimal amount = prices.stream().filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            Long missing = p.getFrequency() == PeriodicalFrequency.DAILY && p.isActive()
                    ? Math.max(0, daysSoFar - issues) : null;
            rows.add(new ReportRow(p.getId(), p.getName(), p.getFrequency(), issues, amount, missing, prices));
            totalIssues += issues;
            totalAmount = totalAmount.add(amount);
        }
        return new MonthReport(month.toString(), from, to, rows, totalIssues, totalAmount);
    }

    private void apply(Periodical p, PeriodicalRequest r) {
        String name = r.name() == null ? "" : r.name().trim();
        if (name.isEmpty() || name.length() > 120) {
            throw bad("Enter the name (up to 120 characters).");
        }
        if (!name.equalsIgnoreCase(p.getName()) && periodicals.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "\"" + name + "\" is already in the list.");
        }
        if (r.frequency() == null) {
            throw bad("Choose how often it comes.");
        }
        String language = r.language() == null ? null : r.language().trim();
        p.setName(name);
        p.setLanguage(language == null || language.isEmpty() ? null : language);
        p.setFrequency(r.frequency());
        p.setPrice(price(r.price() == null ? BigDecimal.ZERO : r.price()));
        p.setActive(r.active() == null || r.active());
    }

    private static BigDecimal price(BigDecimal value) {
        if (value.signum() < 0 || value.compareTo(MAX_PRICE) > 0) {
            throw bad("Enter a price between 0 and 10,000.");
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Periodical find(Long id) {
        return periodicals.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Newspaper or magazine not found"));
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
