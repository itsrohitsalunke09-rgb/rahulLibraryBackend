package org.ngolibrary.repo;

import java.util.List;

import org.ngolibrary.domain.Periodical;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodicalRepository extends JpaRepository<Periodical, Long> {
    /** Subscribed ones first, then by name. */
    List<Periodical> findAllByOrderByActiveDescNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
