package org.ngolibrary.repo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.ngolibrary.domain.PeriodicalArrival;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PeriodicalArrivalRepository extends JpaRepository<PeriodicalArrival, Long> {

    List<PeriodicalArrival> findByDate(LocalDate date);

    boolean existsByPeriodicalId(Long periodicalId);

    /** Copies received and amount spent per newspaper/magazine between two dates (inclusive). */
    @Query("SELECT a.periodical.id AS periodicalId, count(a) AS issues, coalesce(sum(a.price), 0) AS amount "
            + "FROM PeriodicalArrival a WHERE a.date BETWEEN :from AND :to GROUP BY a.periodical.id")
    List<Totals> totalsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    interface Totals {
        Long getPeriodicalId();
        long getIssues();
        BigDecimal getAmount();
    }
}
