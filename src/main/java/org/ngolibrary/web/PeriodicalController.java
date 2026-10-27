package org.ngolibrary.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.ngolibrary.domain.Periodical;
import org.ngolibrary.dto.PeriodicalDtos.DayRegister;
import org.ngolibrary.dto.PeriodicalDtos.MonthReport;
import org.ngolibrary.dto.PeriodicalDtos.PeriodicalRequest;
import org.ngolibrary.dto.PeriodicalDtos.RegisterEntry;
import org.ngolibrary.service.PeriodicalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Newspapers & magazines and their daily arrival register. Staff only. */
@RestController
@RequestMapping("/api/periodicals")
@PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
public class PeriodicalController {

    private final PeriodicalService service;

    public PeriodicalController(PeriodicalService service) {
        this.service = service;
    }

    @GetMapping
    public List<Periodical> list() {
        return service.list();
    }

    @PostMapping
    public Periodical create(@RequestBody PeriodicalRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Periodical update(@PathVariable Long id, @RequestBody PeriodicalRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/register")
    public DayRegister register(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.register(date);
    }

    @PutMapping("/register")
    public DayRegister saveRegister(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestBody List<RegisterEntry> entries) {
        return service.saveRegister(date, entries);
    }

    /** month as YYYY-MM. */
    @GetMapping("/report")
    public MonthReport report(@RequestParam String month) {
        return service.report(YearMonth.parse(month));
    }
}
