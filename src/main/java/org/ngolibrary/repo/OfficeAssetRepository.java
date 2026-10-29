package org.ngolibrary.repo;

import java.util.List;

import org.ngolibrary.domain.OfficeAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeAssetRepository extends JpaRepository<OfficeAsset, Long> {

    List<OfficeAsset> findAllByOrderByNameAsc();
}
