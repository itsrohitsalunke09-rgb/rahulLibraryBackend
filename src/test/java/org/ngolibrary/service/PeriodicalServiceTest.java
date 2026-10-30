package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ngolibrary.domain.MagazineReceipt;
import org.ngolibrary.domain.Periodical;
import org.ngolibrary.domain.PeriodicalArrival;
import org.ngolibrary.domain.PeriodicalFrequency;
import org.ngolibrary.dto.PeriodicalDtos.MagazineCard;
import org.ngolibrary.dto.PeriodicalDtos.MonthReport;
import org.ngolibrary.dto.PeriodicalDtos.PeriodicalRequest;
import org.ngolibrary.dto.PeriodicalDtos.ReceiptRequest;
import org.ngolibrary.dto.PeriodicalDtos.RegisterEntry;
import org.ngolibrary.repo.MagazineReceiptRepository;
import org.ngolibrary.repo.PeriodicalArrivalRepository;
import org.ngolibrary.repo.PeriodicalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PeriodicalServiceTest {

    private PeriodicalRepository periodicals;
    private PeriodicalArrivalRepository arrivals;
    private MagazineReceiptRepository receipts;
    private PeriodicalService service;
    private Periodical sakal;
    private Periodical lokrajya;

    @BeforeEach
    void setUp() {
        periodicals = mock(PeriodicalRepository.class);
        arrivals = mock(PeriodicalArrivalRepository.class);
        receipts = mock(MagazineReceiptRepository.class);
        when(receipts.save(any(MagazineReceipt.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new PeriodicalService(periodicals, arrivals, receipts);
        sakal = periodical(1L, "सकाळ", PeriodicalFrequency.DAILY, "6.00", true);
        lokrajya = periodical(2L, "लोकराज्य", PeriodicalFrequency.MONTHLY, "20.00", true);
        when(periodicals.findAllByOrderByActiveDescNameAsc()).thenReturn(List.of(sakal, lokrajya));
        when(periodicals.findById(1L)).thenReturn(Optional.of(sakal));
        when(periodicals.findById(2L)).thenReturn(Optional.of(lokrajya));
        when(periodicals.save(any(Periodical.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Periodical periodical(Long id, String name, PeriodicalFrequency frequency, String price, boolean active) {
        Periodical p = new Periodical();
        p.setId(id);
        p.setName(name);
        p.setFrequency(frequency);
        p.setPrice(new BigDecimal(price));
        p.setActive(active);
        return p;
    }

    private static PeriodicalArrival arrival(Periodical p, LocalDate date, String price) {
        PeriodicalArrival a = new PeriodicalArrival();
        a.setPeriodical(p);
        a.setDate(date);
        a.setPrice(new BigDecimal(price));
        return a;
    }

    private static void assertStatus(HttpStatus status, Runnable call) {
        assertEquals(status, assertThrows(ResponseStatusException.class, call::run).getStatus());
    }

    @Test
    void registerListsOnlyDailyPapersWithTheirPrice() {
        LocalDate day = LocalDate.of(2026, 10, 25);
        when(arrivals.findByDate(day)).thenReturn(List.of(arrival(sakal, day, "8.00")));
        var rows = service.register(day).rows();
        assertEquals(1, rows.size(), "the monthly magazine is in the magazine register instead");
        assertTrue(rows.get(0).arrived());
        assertEquals(new BigDecimal("8.00"), rows.get(0).price(), "the Sunday price recorded that day");
    }

    @Test
    void unTickedRowsOfferTheUsualPrice() {
        LocalDate day = LocalDate.of(2026, 10, 26);
        when(arrivals.findByDate(day)).thenReturn(List.of());
        var row = service.register(day).rows().get(0);
        assertFalse(row.arrived());
        assertEquals(new BigDecimal("6.00"), row.price());
    }

    @Test
    void aMagazineCannotBeTickedInTheDailyRegister() {
        LocalDate day = LocalDate.of(2026, 10, 25);
        when(arrivals.findByDate(day)).thenReturn(List.of());
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.saveRegister(day, List.of(new RegisterEntry(2L, true, null, null))));
        verify(arrivals, never()).save(any());
    }

    @Test
    void savingAddsTickedCopiesAndRemovesUnticked() {
        LocalDate day = LocalDate.of(2026, 10, 25);
        PeriodicalArrival old = arrival(lokrajya, day, "20.00");
        when(arrivals.findByDate(day)).thenReturn(List.of(old));
        service.saveRegister(day, List.of(new RegisterEntry(1L, true, null, null), new RegisterEntry(2L, false, null, null)));

        ArgumentCaptor<PeriodicalArrival> saved = ArgumentCaptor.forClass(PeriodicalArrival.class);
        verify(arrivals).save(saved.capture());
        assertEquals(sakal, saved.getValue().getPeriodical());
        assertEquals(new BigDecimal("6.00"), saved.getValue().getPrice(), "no price given: the usual one");
        verify(arrivals).delete(old);
    }

    @Test
    void futureDatesAndNegativePricesAreRefused() {
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.saveRegister(LocalDate.now().plusDays(1), List.of(new RegisterEntry(1L, true, null, null))));
        LocalDate day = LocalDate.of(2026, 10, 25);
        when(arrivals.findByDate(day)).thenReturn(List.of());
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.saveRegister(day, List.of(new RegisterEntry(1L, true, new BigDecimal("-1"), null))));
        verify(arrivals, never()).save(any());
    }

    @Test
    void reportTotalsCostAndCountsMissingDaysForDailyPapers() {
        YearMonth month = YearMonth.of(2026, 9); // a past month: all 30 days count
        List<PeriodicalArrival> copies = new java.util.ArrayList<>();
        for (int day = 1; day <= 28; day++) { // Sakal came every day but the 29th and 30th
            copies.add(arrival(sakal, month.atDay(day), day % 7 == 0 ? "8.00" : "6.00"));
        }
        copies.add(arrival(lokrajya, month.atDay(5), "20.00"));
        when(arrivals.findByDateBetween(month.atDay(1), month.atEndOfMonth())).thenReturn(copies);

        MonthReport report = service.report(month);
        assertEquals(1, report.rows().size(), "magazines are paid yearly, so they are not in the bill check");
        assertEquals(28, report.totalIssues());
        assertEquals(new BigDecimal("176.00"), report.rows().get(0).amount(), "24 × 6 + 4 Sundays × 8");
        assertEquals(new BigDecimal("176.00"), report.totalAmount());
        assertEquals(2L, report.rows().get(0).missingDays(), "30 days, 28 copies");

        List<BigDecimal> sakalDays = report.rows().get(0).prices();
        assertEquals(30, sakalDays.size(), "one entry per day of September");
        assertEquals(new BigDecimal("8.00"), sakalDays.get(6), "the 7th");
        assertNull(sakalDays.get(29), "no copy on the 30th");
    }

    @Test
    void extraCopiesMultiplyTheDaysAmountAndCountAsIssues() {
        YearMonth month = YearMonth.of(2026, 9);
        PeriodicalArrival two = arrival(sakal, month.atDay(1), "6.00");
        two.setCopies(2);
        when(arrivals.findByDateBetween(month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(two, arrival(sakal, month.atDay(2), "6.00")));

        MonthReport report = service.report(month);
        assertEquals(3, report.totalIssues(), "2 copies on the 1st, 1 on the 2nd");
        assertEquals(new BigDecimal("18.00"), report.totalAmount());
        assertEquals(new BigDecimal("12.00"), report.rows().get(0).prices().get(0));
        assertEquals(28L, report.rows().get(0).missingDays(), "two days came, 28 missed");
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.saveRegister(month.atDay(3), List.of(new RegisterEntry(1L, true, null, 0))));
    }

    @Test
    void aPaperWithRegisterEntriesCannotBeDeleted() {
        when(arrivals.existsByPeriodicalId(1L)).thenReturn(true);
        assertStatus(HttpStatus.CONFLICT, () -> service.delete(1L));
        verify(periodicals, never()).deleteById(any());
    }

    @Test
    void namesMustBeUniqueAndPricesSane() {
        when(periodicals.existsByNameIgnoreCase("लोकमत")).thenReturn(true);
        assertStatus(HttpStatus.CONFLICT, () -> service.create(
                new PeriodicalRequest(" लोकमत ", "मराठी", PeriodicalFrequency.DAILY, new BigDecimal("6"), true, null, null, null, null)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.create(
                new PeriodicalRequest("Some paper", null, PeriodicalFrequency.DAILY, new BigDecimal("20000"), true, null, null, null, null)));
        Periodical created = service.create(new PeriodicalRequest(" India Today ", " ", PeriodicalFrequency.WEEKLY, null, null, null, null, null, null));
        assertEquals("India Today", created.getName());
        assertNull(created.getLanguage());
        assertEquals(new BigDecimal("0.00"), created.getPrice());
        assertTrue(created.isActive());
    }

    @Test
    void aMagazineSubscriptionRunsAYearFromItsStartByDefault() {
        Periodical created = service.create(new PeriodicalRequest("India Today", null, PeriodicalFrequency.WEEKLY,
                null, true, new BigDecimal("2500"), LocalDate.of(2026, 4, 1), null, null));
        assertEquals(new BigDecimal("2500.00"), created.getSubscriptionAmount());
        assertEquals(LocalDate.of(2027, 3, 31), created.getSubscriptionEnd());
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.create(new PeriodicalRequest("Outlook", null,
                PeriodicalFrequency.WEEKLY, null, true, null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 1), null)));
    }

    @Test
    void aContactNumberIsOptionalButMustLookLikeAPhoneNumber() {
        Periodical created = service.create(new PeriodicalRequest("सकाळ", null, PeriodicalFrequency.DAILY,
                new BigDecimal("6"), true, null, null, null, " +91 98765-43210 "));
        assertEquals("+91 98765-43210", created.getContactNumber());
        assertNull(service.create(new PeriodicalRequest("लोकमत", null, PeriodicalFrequency.DAILY,
                new BigDecimal("6"), true, null, null, null, " ")).getContactNumber());
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.create(new PeriodicalRequest("पुढारी", null,
                PeriodicalFrequency.DAILY, new BigDecimal("6"), true, null, null, null, "call raju")));
    }

    @Test
    void aDailyPaperKeepsNoSubscription() {
        Periodical created = service.create(new PeriodicalRequest("लोकसत्ता", null, PeriodicalFrequency.DAILY,
                new BigDecimal("6"), true, new BigDecimal("2500"), LocalDate.of(2026, 4, 1), null, null));
        assertNull(created.getSubscriptionAmount());
        assertNull(created.getSubscriptionStart());
    }

    @Test
    void anIssueIsRecordedWithItsNumber() {
        LocalDate published = LocalDate.of(2026, 10, 5);
        var receipt = service.addReceipt(2L, new ReceiptRequest(12, published, published, published, null));
        assertEquals(12, receipt.issueNo());
        assertEquals(published, receipt.periodFrom());
    }

    @Test
    void aWeeklysIssueNumberCannotBeMarkedTwiceButAMonthlysCan() {
        Periodical indiaToday = periodical(3L, "India Today", PeriodicalFrequency.WEEKLY, "60.00", true);
        when(periodicals.findById(3L)).thenReturn(Optional.of(indiaToday));
        when(receipts.existsByPeriodicalIdAndIssueNo(3L, 12)).thenReturn(true);
        when(receipts.existsByPeriodicalIdAndIssueNo(2L, 12)).thenReturn(true);
        LocalDate day = LocalDate.of(2026, 10, 5);
        assertStatus(HttpStatus.CONFLICT, () -> service.addReceipt(3L, new ReceiptRequest(12, day, day, day.plusDays(6), null)));
        verify(receipts, never()).save(any());
        // Monthly: अंक 12 again is fine, for a month with no अंक yet.
        assertEquals(12, service.addReceipt(2L, new ReceiptRequest(12, day, day.withDayOfMonth(1), day.withDayOfMonth(31), null)).issueNo());
    }

    @Test
    void oneIssuePerWeekOrPublishedDate() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        when(receipts.existsByPeriodicalIdAndPeriodFromLessThanEqualAndPeriodToGreaterThanEqual(2L, to, from)).thenReturn(true);
        assertStatus(HttpStatus.CONFLICT, () -> service.addReceipt(2L, new ReceiptRequest(13, from, from, to, null)));
        verify(receipts, never()).save(any());
    }

    @Test
    void receiptsAreCheckedBeforeSaving() {
        LocalDate today = LocalDate.now();
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.addReceipt(1L, new ReceiptRequest(1, today, today, today, null)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.addReceipt(2L, new ReceiptRequest(1, today.plusDays(1), today, today, null)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.addReceipt(2L, new ReceiptRequest(1, today, today, today.minusDays(1), null)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.addReceipt(2L, new ReceiptRequest(1, today, null, today, null)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.addReceipt(2L, new ReceiptRequest(null, today, today, today, null)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.addReceipt(2L, new ReceiptRequest(0, today, today, today, null)));
        verify(receipts, never()).save(any());
    }

    @Test
    void theMagazineRegisterGroupsReceiptsPerMagazine() {
        MagazineReceipt october = new MagazineReceipt();
        october.setPeriodical(lokrajya);
        october.setReceivedOn(LocalDate.of(2026, 10, 6));
        october.setPeriodFrom(LocalDate.of(2026, 10, 1));
        october.setPeriodTo(LocalDate.of(2026, 10, 31));
        when(receipts.findAllByOrderByPeriodFromAsc()).thenReturn(List.of(october));

        List<MagazineCard> cards = service.magazines();
        assertEquals(1, cards.size(), "the daily paper is not a magazine");
        assertEquals("लोकराज्य", cards.get(0).name());
        assertEquals(LocalDate.of(2026, 10, 1), cards.get(0).receipts().get(0).periodFrom());
    }

    @Test
    void aMagazineWithReceiptsCannotBeDeleted() {
        when(receipts.existsByPeriodicalId(2L)).thenReturn(true);
        assertStatus(HttpStatus.CONFLICT, () -> service.delete(2L));
        verify(periodicals, never()).deleteById(any());
    }
}
