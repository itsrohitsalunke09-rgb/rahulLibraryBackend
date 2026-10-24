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
    long countByRole(Role role);
    List<UserAccount> findByEmailIgnoreCaseAndActiveTrue(String email);

    @Query("SELECT u FROM UserAccount u WHERE u.role = :role")
    Page<UserAccount> findByRole(@Param("role") Role role, Pageable pageable);

    @Query("SELECT u FROM UserAccount u WHERE u.role = 'STUDENT' AND u.active = true AND "
            + "(lower(u.fullName) LIKE lower(concat(:q, '%')) "
            + "OR lower(u.username) LIKE lower(concat(:q, '%')) "
            + "OR lower(u.email) LIKE lower(concat(:q, '%')) "
            + "OR lower(u.phone) LIKE lower(concat(:q, '%')))")
    List<UserAccount> searchStudents(@Param("q") String q);

    /** Accounts of the given roles (active or not) whose name, username, email or phone starts with q. */
    @Query("SELECT u FROM UserAccount u WHERE u.role IN :roles AND "
            + "(lower(u.fullName) LIKE lower(concat(:q, '%')) "
            + "OR lower(u.username) LIKE lower(concat(:q, '%')) "
            + "OR lower(u.email) LIKE lower(concat(:q, '%')) "
            + "OR lower(u.phone) LIKE lower(concat(:q, '%')))")
    Page<UserAccount> search(@Param("roles") List<Role> roles, @Param("q") String q, Pageable pageable);
}
