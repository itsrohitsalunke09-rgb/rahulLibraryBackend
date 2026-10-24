package org.ngolibrary.repo;

import org.ngolibrary.domain.NewsStory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsStoryRepository extends JpaRepository<NewsStory, Long> {
    Page<NewsStory> findAllByOrderByEventDateDescIdDesc(Pageable pageable);
    Page<NewsStory> findByHeadlineContainingIgnoreCaseOrderByEventDateDescIdDesc(String q, Pageable pageable);
}
