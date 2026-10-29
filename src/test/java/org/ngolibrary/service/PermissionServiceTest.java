package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.Permission;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.RolePermission;
import org.ngolibrary.repo.RolePermissionRepository;

class PermissionServiceTest {

    private final RolePermissionRepository repository = mock(RolePermissionRepository.class);
    private final PermissionService service = new PermissionService(repository);

    @Test
    void adminAlwaysHasEverythingAndOthersFollowTheirTicks() {
        when(repository.count()).thenReturn(1L);
        when(repository.findAll()).thenReturn(List.of(new RolePermission(Role.LIBRARIAN, Permission.DELETE)));
        service.load();
        assertTrue(service.has(Role.ADMIN, Permission.DELETE));
        assertTrue(service.has(Role.LIBRARIAN, Permission.DELETE));
        assertFalse(service.has(Role.LIBRARIAN, Permission.READ));
        assertFalse(service.has(Role.STUDENT, Permission.READ));
        assertEquals(EnumSet.allOf(Permission.class), service.matrix().get(Role.ADMIN));
    }

    @Test
    void savingChangesTheTicksButNeverAdmins() {
        Map<Role, Set<Permission>> saved = service.update(Map.of(
                Role.STUDENT, Set.of(Permission.READ, Permission.GALAXY_VIEW),
                Role.ADMIN, Set.of()));
        assertTrue(service.has(Role.STUDENT, Permission.GALAXY_VIEW));
        assertFalse(service.has(Role.LIBRARIAN, Permission.READ));
        assertEquals(EnumSet.allOf(Permission.class), saved.get(Role.ADMIN));
    }

    @Test
    void startsFromTheDefaults() {
        assertTrue(service.has(Role.LIBRARIAN, Permission.WRITE));
        assertFalse(service.has(Role.LIBRARIAN, Permission.DELETE));
        assertTrue(service.has(Role.STUDENT, Permission.READ));
        assertFalse(service.has(Role.STUDENT, Permission.WRITE));
    }
}
