package org.ngolibrary.security;

import org.ngolibrary.domain.Permission;
import org.ngolibrary.domain.Role;
import org.ngolibrary.service.PermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** For @PreAuthorize: "@perm.can('DELETE')" is true when the signed-in user's role has that permission. */
@Component("perm")
public class PermissionChecker {

    private final PermissionService permissions;

    public PermissionChecker(PermissionService permissions) {
        this.permissions = permissions;
    }

    public boolean can(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Permission wanted = Permission.valueOf(permission);
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String name = authority.getAuthority();
            if (name.startsWith("ROLE_")) {
                try {
                    if (permissions.has(Role.valueOf(name.substring(5)), wanted)) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    // Not one of our roles.
                }
            }
        }
        return false;
    }
}
