package org.ngolibrary.web;

import java.util.Map;
import java.util.Set;

import org.ngolibrary.domain.Permission;
import org.ngolibrary.domain.Role;
import org.ngolibrary.service.PermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Roles and permissions master. Anyone signed in reads it (the app shows or hides actions by it); admins change it. */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissions;

    public PermissionController(PermissionService permissions) {
        this.permissions = permissions;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Map<Role, Set<Permission>> matrix() {
        return permissions.matrix();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<Role, Set<Permission>> update(@RequestBody Map<Role, Set<Permission>> ticks) {
        return permissions.update(ticks);
    }
}
