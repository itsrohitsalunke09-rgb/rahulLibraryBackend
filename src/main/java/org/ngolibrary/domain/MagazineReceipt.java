package org.ngolibrary.domain;

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

/**
 * One weekly/fortnightly/monthly magazine issue received: its issue number (अंक) and the period it
 * covers. Monthly issues are dated by the day they were published; weekly ones by their from–to week.
 */
@Entity
@Table(name = "magazine_receipts")
public class MagazineReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "periodical_id")
    private Periodical periodical;

    /** The issue number printed on it (अंक 1, 2, 3…), unique per magazine. */
    @Column(name = "issue_no")
    private Integer issueNo;

    @Column(name = "received_on", nullable = false)
    private LocalDate receivedOn;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Periodical getPeriodical() {
        return periodical;
    }

    public void setPeriodical(Periodical periodical) {
        this.periodical = periodical;
    }

    public Integer getIssueNo() {
        return issueNo;
    }

    public void setIssueNo(Integer issueNo) {
        this.issueNo = issueNo;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public void setReceivedOn(LocalDate receivedOn) {
        this.receivedOn = receivedOn;
    }

    public LocalDate getPeriodFrom() {
        return periodFrom;
    }

    public void setPeriodFrom(LocalDate periodFrom) {
        this.periodFrom = periodFrom;
    }

    public LocalDate getPeriodTo() {
        return periodTo;
    }

    public void setPeriodTo(LocalDate periodTo) {
        this.periodTo = periodTo;
    }
}
