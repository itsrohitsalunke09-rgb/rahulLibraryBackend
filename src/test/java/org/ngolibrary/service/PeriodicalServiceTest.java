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
import org.ngolibrary.domain.Periodical;
import org.ngolibrary.domain.PeriodicalArrival;
import org.ngolibrary.domain.PeriodicalFrequency;
import org.ngolibrary.dto.PeriodicalDtos.MonthReport;
import org.ngolibrary.dto.PeriodicalDtos.PeriodicalRequest;
import org.ngolibrary.dto.PeriodicalDtos.RegisterEntry;
import org.ngolibrary.repo.PeriodicalArrivalRepository;
import org.ngolibrary.repo.PeriodicalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PeriodicalServiceTest {

    private PeriodicalRepository periodicals;
    private PeriodicalArrivalRepository arrivals;
    private PeriodicalService service;
    private Periodical sakal;
    private Periodical lokrajya;

    @BeforeEach
    void setUp() {
        periodicals = mock(PeriodicalRepository.class);
        arrivals = mock(PeriodicalArrivalRepository.class);
        service = new PeriodicalService(periodicals, arrivals);
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
    void registerListsSubscribedPapersWithTheirUsualPrice() {
        LocalDate day = LocalDate.of(2026, 10, 25);
        when(arrivals.findByDate(day)).thenReturn(List.of(arrival(sakal, day, "8.00")));
        var rows = service.register(day).rows();
        assertEquals(2, rows.size());
        assertTrue(rows.get(0).arrived());
        assertEquals(new BigDecimal("8.00"), rows.get(0).price(), "the Sunday price recorded that day");
        assertFalse(rows.get(1).arrived());
        assertEquals(new BigDecimal("20.00"), rows.get(1).price(), "unticked rows offer the usual price");
    }

    @Test
    void savingAddsTickedCopiesAndRemovesUnticked() {
        LocalDate day = LocalDate.of(2026, 10, 25);
        PeriodicalArrival old = arrival(lokrajya, day, "20.00");
        when(arrivals.findByDate(day)).thenReturn(List.of(old));
        service.saveRegister(day, List.of(new RegisterEntry(1L, true, null), new RegisterEntry(2L, false, null)));

        ArgumentCaptor<PeriodicalArrival> saved = ArgumentCaptor.forClass(PeriodicalArrival.class);
        verify(arrivals).save(saved.capture());
        assertEquals(sakal, saved.getValue().getPeriodical());
        assertEquals(new BigDecimal("6.00"), saved.getValue().getPrice(), "no price given: the usual one");
        verify(arrivals).delete(old);
    }

    @Test
    void futureDatesAndNegativePricesAreRefused() {
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.saveRegister(LocalDate.now().plusDays(1), List.of(new RegisterEntry(1L, true, null))));
        LocalDate day = LocalDate.of(2026, 10, 25);
        when(arrivals.findByDate(day)).thenReturn(List.of());
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.saveRegister(day, List.of(new RegisterEntry(1L, true, new BigDecimal("-1")))));
        verify(arrivals, never()).save(any());
    }

    @Test
    void reportTotalsCostAndCountsMissingDaysForDailyPapers() {
        YearMonth month = YearMonth.of(2026, 9); // a past month: all 30 days count
        when(arrivals.totalsBetween(month.atDay(1), month.atEndOfMonth())).thenReturn(List.of(
                totals(1L, 28, "180.00"), totals(2L, 1, "20.00")));
        MonthReport report = service.report(month);
        assertEquals(29, report.totalIssues());
        assertEquals(new BigDecimal("200.00"), report.totalAmount());
        assertEquals(2L, report.rows().get(0).missingDays(), "30 days, 28 copies");
        assertNull(report.rows().get(1).missingDays(), "monthly magazines have no missing days");
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
                new PeriodicalRequest(" लोकमत ", "मराठी", PeriodicalFrequency.DAILY, new BigDecimal("6"), true)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.create(
                new PeriodicalRequest("Some paper", null, PeriodicalFrequency.DAILY, new BigDecimal("20000"), true)));
        Periodical created = service.create(new PeriodicalRequest(" India Today ", " ", PeriodicalFrequency.WEEKLY, null, null));
        assertEquals("India Today", created.getName());
        assertNull(created.getLanguage());
        assertEquals(new BigDecimal("0.00"), created.getPrice());
        assertTrue(created.isActive());
    }

    private static PeriodicalArrivalRepository.Totals totals(Long id, long issues, String amount) {
        return new PeriodicalArrivalRepository.Totals() {
            public Long getPeriodicalId() { return id; }
            public long getIssues() { return issues; }
            public BigDecimal getAmount() { return new BigDecimal(amount); }
        };
    }
}
