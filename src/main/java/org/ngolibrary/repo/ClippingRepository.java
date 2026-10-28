package org.ngolibrary.repo;

import java.util.List;

import org.ngolibrary.domain.Clipping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClippingRepository extends JpaRepository<Clipping, Long> {
    List<Clipping> findAllByOrderByYearFromDescTitleAsc();
}
