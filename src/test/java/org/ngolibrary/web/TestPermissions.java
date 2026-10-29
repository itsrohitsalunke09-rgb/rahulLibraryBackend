package org.ngolibrary.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.ngolibrary.domain.Permission;
import org.ngolibrary.domain.Role;
import org.ngolibrary.service.PermissionService;

/** For controller tests: a mocked PermissionService that answers with the default ticks. */
final class TestPermissions {

    private TestPermissions() {
    }

    static void defaults(PermissionService permissions) {
        when(permissions.has(any(Role.class), any(Permission.class))).thenAnswer(call -> {
            Role role = call.getArgument(0);
            return role == Role.ADMIN || PermissionService.DEFAULTS.getOrDefault(role, java.util.Set.of()).contains(call.getArgument(1));
        });
    }
}
