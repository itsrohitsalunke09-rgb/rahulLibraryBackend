package org.ngolibrary.repo;

import java.time.LocalDate;
import java.util.List;

import org.ngolibrary.domain.MagazineReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MagazineReceiptRepository extends JpaRepository<MagazineReceipt, Long> {

    /** Every receipt, oldest period first; the register groups them per magazine. */
    List<MagazineReceipt> findAllByOrderByPeriodFromAsc();

    boolean existsByPeriodicalId(Long periodicalId);

    boolean existsByPeriodicalIdAndIssueNo(Long periodicalId, Integer issueNo);

    /** A receipt whose period overlaps [from, to]: called with (id, to, from). */
    boolean existsByPeriodicalIdAndPeriodFromLessThanEqualAndPeriodToGreaterThanEqual(Long periodicalId, LocalDate to, LocalDate from);
}
