package org.ngolibrary.web;

import java.util.List;

import javax.validation.Valid;

import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.ProfileUpdateRequest;
import org.ngolibrary.dto.UserRequest;
import org.ngolibrary.dto.UserResponse;
import org.ngolibrary.service.AuthService;
import org.ngolibrary.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Page<UserResponse> list(@RequestParam(required = false) Role role, Pageable pageable) {
        return userService.listPaged(role, pageable);
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public List<UserResponse> searchStudents(@RequestParam(required = false) String q) {
        return userService.searchStudents(q);
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public UserResponse getProfile(Authentication authentication) {
        UserAccount currentUser = authService.current(authentication.getName());
        return UserResponse.from(currentUser);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        UserAccount currentUser = authService.current(authentication.getName());
        return userService.updateProfile(currentUser.getId(), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
