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

import org.ngolibrary.domain.MagazineReceipt;
import org.ngolibrary.domain.Periodical;
import org.ngolibrary.domain.PeriodicalArrival;
import org.ngolibrary.domain.PeriodicalFrequency;
import org.ngolibrary.dto.PeriodicalDtos.DayRegister;
import org.ngolibrary.dto.PeriodicalDtos.MagazineCard;
import org.ngolibrary.dto.PeriodicalDtos.MonthReport;
import org.ngolibrary.dto.PeriodicalDtos.PeriodicalRequest;
import org.ngolibrary.dto.PeriodicalDtos.PeriodicalSummary;
import org.ngolibrary.dto.PeriodicalDtos.ReceiptRequest;
import org.ngolibrary.dto.PeriodicalDtos.ReceiptView;
import org.ngolibrary.dto.PeriodicalDtos.RegisterEntry;
import org.ngolibrary.dto.PeriodicalDtos.RegisterRow;
import org.ngolibrary.dto.PeriodicalDtos.ReportRow;
import org.ngolibrary.repo.MagazineReceiptRepository;
import org.ngolibrary.repo.PeriodicalArrivalRepository;
import org.ngolibrary.repo.PeriodicalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Newspapers & magazines. Daily papers go in the daily register (a tick and a price per day) and the
 * monthly report; weekly/fortnightly/monthly magazines are paid yearly and go in the magazine register,
 * where each issue received is recorded with the period it covers.
 */
@Service
public class PeriodicalService {

    static final BigDecimal MAX_PRICE = new BigDecimal("10000");
    static final BigDecimal MAX_SUBSCRIPTION = new BigDecimal("1000000");

    private final PeriodicalRepository periodicals;
    private final PeriodicalArrivalRepository arrivals;
    private final MagazineReceiptRepository receipts;

    public PeriodicalService(PeriodicalRepository periodicals, PeriodicalArrivalRepository arrivals,
                             MagazineReceiptRepository receipts) {
        this.periodicals = periodicals;
        this.arrivals = arrivals;
        this.receipts = receipts;
    }

    private static boolean daily(Periodical p) {
        return p.getFrequency() == PeriodicalFrequency.DAILY;
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
        if (arrivals.existsByPeriodicalId(id) || receipts.existsByPeriodicalId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This one has register entries. Switch off \"Subscribed\" instead, so its history stays.");
        }
        periodicals.deleteById(id);
    }

    /** The day's register: every subscribed daily paper, plus any old one that has an entry that day. */
    @Transactional(readOnly = true)
    public DayRegister register(LocalDate date) {
        Map<Long, PeriodicalArrival> entries = arrivals.findByDate(date).stream()
                .collect(Collectors.toMap(a -> a.getPeriodical().getId(), Function.identity()));
        List<RegisterRow> rows = new ArrayList<>();
        for (Periodical p : list()) {
            PeriodicalArrival entry = entries.get(p.getId());
            if (daily(p) && (p.isActive() || entry != null)) {
                rows.add(new RegisterRow(p.getId(), p.getName(), p.getLanguage(), p.getFrequency(), p.getPrice(),
                        entry != null, entry != null ? entry.getPrice() : p.getPrice(), p.getContactNumber()));
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
            if (!daily(p)) {
                throw bad("\"" + p.getName() + "\" is a magazine: record it in the magazine register.");
            }
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
     * Copies and cost per daily paper for a month. It also counts the days
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
            if (!daily(p) || (!p.isActive() && days == null)) {
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

    /** The magazine register: every subscribed magazine (and any old one with issues), each with its receipts. */
    @Transactional(readOnly = true)
    public List<MagazineCard> magazines() {
        Map<Long, List<ReceiptView>> byMagazine = new java.util.HashMap<>();
        for (MagazineReceipt r : receipts.findAllByOrderByPeriodFromAsc()) {
            byMagazine.computeIfAbsent(r.getPeriodical().getId(), id -> new ArrayList<>()).add(view(r));
        }
        return list().stream()
                .filter(p -> !daily(p) && (p.isActive() || byMagazine.containsKey(p.getId())))
                .map(p -> new MagazineCard(p.getId(), p.getName(), p.getLanguage(), p.getFrequency(),
                        p.getSubscriptionAmount(), p.getSubscriptionStart(), p.getSubscriptionEnd(),
                        byMagazine.getOrDefault(p.getId(), List.of()), p.getContactNumber()))
                .toList();
    }

    /** Records one issue of a magazine: its number (अंक), and its published date or from–to week. */
    @Transactional
    public ReceiptView addReceipt(Long periodicalId, ReceiptRequest r) {
        Periodical p = find(periodicalId);
        if (daily(p)) {
            throw bad("\"" + p.getName() + "\" is a daily paper: tick it in the daily register.");
        }
        if (r == null || r.receivedOn() == null || r.periodFrom() == null || r.periodTo() == null) {
            throw bad("Enter the issue's dates.");
        }
        if (r.issueNo() == null || r.issueNo() < 1 || r.issueNo() > 99999) {
            throw bad("Enter the issue number (अंक), e.g. 12.");
        }
        // A monthly's अंक numbers may repeat (some restart them); a weekly's or fortnightly's may not.
        if (p.getFrequency() != PeriodicalFrequency.MONTHLY
                && receipts.existsByPeriodicalIdAndIssueNo(periodicalId, r.issueNo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "अंक " + r.issueNo() + " of \"" + p.getName() + "\" is already marked received.");
        }
        if (r.receivedOn().isAfter(LocalDate.now())) {
            throw bad("The date can't be in the future.");
        }
        if (r.periodTo().isBefore(r.periodFrom())) {
            throw bad("The period must end on or after its start.");
        }
        // One अंक per week, fortnight or month: a period that overlaps one already received is a duplicate.
        if (receipts.existsByPeriodicalIdAndPeriodFromLessThanEqualAndPeriodToGreaterThanEqual(
                periodicalId, r.periodTo(), r.periodFrom())) {
            String period = p.getFrequency() == PeriodicalFrequency.MONTHLY ? "month"
                    : p.getFrequency() == PeriodicalFrequency.FORTNIGHTLY ? "fortnight" : "week";
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "\"" + p.getName() + "\" already has an अंक marked received for this " + period + ".");
        }
        MagazineReceipt receipt = new MagazineReceipt();
        receipt.setPeriodical(p);
        receipt.setIssueNo(r.issueNo());
        receipt.setReceivedOn(r.receivedOn());
        receipt.setPeriodFrom(r.periodFrom());
        receipt.setPeriodTo(r.periodTo());
        return view(receipts.save(receipt));
    }

    /** Removes a receipt recorded by mistake. */
    public void deleteReceipt(Long id) {
        if (!receipts.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found");
        }
        receipts.deleteById(id);
    }

    private static ReceiptView view(MagazineReceipt r) {
        return new ReceiptView(r.getId(), r.getIssueNo(), r.getReceivedOn(), r.getPeriodFrom(), r.getPeriodTo());
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
        p.setContactNumber(contactNumber(r.contactNumber()));
        applySubscription(p, r);
    }

    /** A phone number to call: digits with optional +, spaces or dashes, 6 to 15 digits; blank means none. */
    static String contactNumber(String value) {
        String number = value == null ? "" : value.trim();
        if (number.isEmpty()) {
            return null;
        }
        long digits = number.chars().filter(Character::isDigit).count();
        if (!number.matches("\\+?[0-9 -]+") || digits < 6 || digits > 15) {
            throw bad("Enter a phone number with 6 to 15 digits, like 98765 43210.");
        }
        return number;
    }

    /** Daily papers are paid per copy; magazines keep a yearly amount and the year it covers. */
    private static void applySubscription(Periodical p, PeriodicalRequest r) {
        if (daily(p)) {
            p.setSubscriptionAmount(null);
            p.setSubscriptionStart(null);
            p.setSubscriptionEnd(null);
            return;
        }
        BigDecimal amount = r.subscriptionAmount();
        if (amount != null && (amount.signum() < 0 || amount.compareTo(MAX_SUBSCRIPTION) > 0)) {
            throw bad("Enter a yearly amount between 0 and 10,00,000.");
        }
        LocalDate start = r.subscriptionStart();
        LocalDate end = r.subscriptionEnd();
        if (start == null && end != null) {
            throw bad("Enter when the subscription starts.");
        }
        if (start != null && end == null) {
            end = start.plusYears(1).minusDays(1);
        }
        if (start != null && end.isBefore(start)) {
            throw bad("The subscription must end after it starts.");
        }
        p.setSubscriptionAmount(amount == null ? null : amount.setScale(2, java.math.RoundingMode.HALF_UP));
        p.setSubscriptionStart(start);
        p.setSubscriptionEnd(end);
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
