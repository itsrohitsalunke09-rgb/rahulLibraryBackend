package org.ngolibrary.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/** One copy of a newspaper or magazine received on a day, and what it cost. */
@Entity
@Table(name = "periodical_arrivals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"periodical_id", "arrival_date"}))
public class PeriodicalArrival {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "periodical_id")
    private Periodical periodical;

    @Column(name = "arrival_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    /** Copies of the paper that came that day (1 unless more were bought). */
    private Integer copies;

    public Long getId() {
        return id;
    }

    public Periodical getPeriodical() {
        return periodical;
    }

    public void setPeriodical(Periodical periodical) {
        this.periodical = periodical;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /** Copies received; null on records saved before copies were kept, which count as one. */
    public int getCopies() {
        return copies == null ? 1 : copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }
}
