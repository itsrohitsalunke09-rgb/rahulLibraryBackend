package org.ngolibrary.repo;

import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role = :role")
    void deleteByRole(@Param("role") Role role);
}
