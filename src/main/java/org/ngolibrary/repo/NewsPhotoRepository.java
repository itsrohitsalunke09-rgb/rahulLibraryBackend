package org.ngolibrary.repo;

import org.ngolibrary.domain.NewsPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsPhotoRepository extends JpaRepository<NewsPhoto, Long> {
}
