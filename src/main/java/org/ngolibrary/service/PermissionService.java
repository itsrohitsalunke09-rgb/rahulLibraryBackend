package org.ngolibrary.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.ngolibrary.domain.Permission;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.RolePermission;
import org.ngolibrary.repo.RolePermissionRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Roles and permissions master: which role may read, write, update, delete and view the Reading Galaxy.
 * Admin always has everything (so no one can be locked out). Kept in memory for the security checks; starts
 * from defaults matching how the app worked before the master existed.
 */
@Service
public class PermissionService {

    /** The roles whose ticks can be changed; admin's are fixed. */
    static final Role[] EDITABLE = {Role.LIBRARIAN, Role.STUDENT};

    /** Librarian: everything but delete; student: read only. */
    public static final Map<Role, Set<Permission>> DEFAULTS = Map.of(
            Role.LIBRARIAN, EnumSet.of(Permission.READ, Permission.WRITE, Permission.UPDATE, Permission.GALAXY_VIEW),
            Role.STUDENT, EnumSet.of(Permission.READ));

    private final RolePermissionRepository repository;
    private volatile Map<Role, Set<Permission>> ticks = copy(DEFAULTS);

    public PermissionService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    /**
     * On start-up: save the defaults the first time, then load what's saved. Admin's rows are saved too, so the
     * table is never empty again (an admin unticking every box doesn't bring the defaults back on restart).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void load() {
        if (repository.count() == 0) {
            DEFAULTS.forEach((role, permissions) -> permissions.forEach(p -> repository.save(new RolePermission(role, p))));
            for (Permission p : Permission.values()) {
                repository.save(new RolePermission(Role.ADMIN, p));
            }
        }
        Map<Role, Set<Permission>> loaded = copy(Map.of());
        repository.findAll().forEach(rp -> {
            if (rp.getRole() != Role.ADMIN) {
                loaded.get(rp.getRole()).add(rp.getPermission());
            }
        });
        ticks = loaded;
    }

    public boolean has(Role role, Permission permission) {
        return role == Role.ADMIN || ticks.getOrDefault(role, Set.of()).contains(permission);
    }

    /** Every role's permissions, admin included (always all). */
    public Map<Role, Set<Permission>> matrix() {
        Map<Role, Set<Permission>> all = copy(ticks);
        all.put(Role.ADMIN, EnumSet.allOf(Permission.class));
        return all;
    }

    /** Saves the librarian and student ticks (anything sent for admin is ignored). */
    @Transactional
    public Map<Role, Set<Permission>> update(Map<Role, Set<Permission>> chosen) {
        for (Role role : EDITABLE) {
            repository.deleteByRole(role);
            chosen.getOrDefault(role, Set.of()).forEach(p -> repository.save(new RolePermission(role, p)));
        }
        Map<Role, Set<Permission>> saved = copy(Map.of());
        for (Role role : EDITABLE) {
            saved.get(role).addAll(chosen.getOrDefault(role, Set.of()));
        }
        ticks = saved;
        return matrix();
    }

    /** Librarian and student entries (empty when missing), as fresh sets. */
    private static Map<Role, Set<Permission>> copy(Map<Role, Set<Permission>> from) {
        Map<Role, Set<Permission>> to = new EnumMap<>(Role.class);
        for (Role role : EDITABLE) {
            Set<Permission> set = EnumSet.noneOf(Permission.class);
            set.addAll(from.getOrDefault(role, Set.of()));
            to.put(role, set);
        }
        return to;
    }
}
