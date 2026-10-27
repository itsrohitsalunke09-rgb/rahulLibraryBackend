package org.ngolibrary.repo;

import java.time.LocalDate;
import java.util.List;

import org.ngolibrary.domain.PeriodicalArrival;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodicalArrivalRepository extends JpaRepository<PeriodicalArrival, Long> {

    List<PeriodicalArrival> findByDate(LocalDate date);

    boolean existsByPeriodicalId(Long periodicalId);

    /** Every copy received between two dates (inclusive), for the monthly report. */
    List<PeriodicalArrival> findByDateBetween(LocalDate from, LocalDate to);
}
