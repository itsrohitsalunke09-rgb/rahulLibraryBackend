package org.ngolibrary.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
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

    /** Librarian: everything but delete and calling vendors; student: read only. */
    public static final Map<Role, Set<Permission>> DEFAULTS = Map.of(
            Role.LIBRARIAN, EnumSet.of(Permission.READ, Permission.WRITE, Permission.UPDATE, Permission.GALAXY_VIEW,
                    Permission.REGISTER),
            Role.STUDENT, EnumSet.of(Permission.READ));

    private final RolePermissionRepository repository;
    private volatile Map<Role, Set<Permission>> ticks = copy(DEFAULTS);

    public PermissionService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    /**
     * On start-up: save each permission's defaults the first time it is seen (the first start, or a permission
     * added since), then load what's saved. Admin's row marks a permission as seen, so an admin unticking
     * every box never brings the defaults back on restart.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void load() {
        repository.renameIssueBookToRegister();
        List<RolePermission> rows = new ArrayList<>(repository.findAll());
        Set<Permission> seen = EnumSet.noneOf(Permission.class);
        rows.stream().filter(rp -> rp.getRole() == Role.ADMIN).forEach(rp -> seen.add(rp.getPermission()));
        for (Permission p : Permission.values()) {
            if (!seen.contains(p)) {
                DEFAULTS.forEach((role, permissions) -> {
                    if (permissions.contains(p)) {
                        rows.add(repository.save(new RolePermission(role, p)));
                    }
                });
                repository.save(new RolePermission(Role.ADMIN, p));
            }
        }
        Map<Role, Set<Permission>> loaded = copy(Map.of());
        rows.forEach(rp -> {
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
