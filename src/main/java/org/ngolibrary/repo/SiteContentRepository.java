package org.ngolibrary.repo;

import org.ngolibrary.domain.SiteContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteContentRepository extends JpaRepository<SiteContent, String> {
}
