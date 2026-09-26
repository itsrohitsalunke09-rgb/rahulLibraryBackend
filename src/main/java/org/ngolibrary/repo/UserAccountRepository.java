package org.ngolibrary.repo;

import java.util.List;
import java.util.Optional;

import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    List<UserAccount> findByRole(Role role);
    List<UserAccount> findByRoleAndActiveTrue(Role role);

    @Query("SELECT u FROM UserAccount u WHERE u.role = :role")
    Page<UserAccount> findByRole(@Param("role") Role role, Pageable pageable);

    @Query("SELECT u FROM UserAccount u WHERE u.role = 'STUDENT' AND u.active = true AND "
            + "(lower(u.fullName) LIKE lower(concat(:q, '%')) "
            + "OR lower(u.username) LIKE lower(concat(:q, '%')) "
            + "OR lower(u.email) LIKE lower(concat(:q, '%')) "
            + "OR lower(u.phone) LIKE lower(concat(:q, '%')))")
    List<UserAccount> searchStudents(@Param("q") String q);
}
