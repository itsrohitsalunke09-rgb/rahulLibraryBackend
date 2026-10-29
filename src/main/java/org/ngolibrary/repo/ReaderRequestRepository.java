package org.ngolibrary.repo;

import java.util.List;

import org.ngolibrary.domain.ReaderRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReaderRequestRepository extends JpaRepository<ReaderRequest, Long> {

    List<ReaderRequest> findAllByOrderByCreatedAtDesc();

    List<ReaderRequest> findAllByKindAndStudentIdOrderByCreatedAtDesc(ReaderRequest.Kind kind, Long studentId);

    boolean existsByKindAndStudentIdAndBookId(ReaderRequest.Kind kind, Long studentId, Long bookId);
}
