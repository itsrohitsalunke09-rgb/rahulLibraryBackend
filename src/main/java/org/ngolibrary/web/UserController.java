package org.ngolibrary.web;

import java.util.List;

import javax.validation.Valid;

import org.ngolibrary.service.PhotoStorage;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
    public Page<UserResponse> list(@RequestParam(required = false) Role role,
                                   @RequestParam(required = false) String q, Pageable pageable) {
        return userService.listPaged(role, q, pageable);
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public List<UserResponse> searchStudents(@RequestParam(required = false) String q) {
        return userService.searchStudents(q);
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public UserResponse getProfile(Authentication authentication) {
        UserAccount currentUser = authService.current(authentication);
        return UserResponse.from(currentUser);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public UserResponse create(@Valid @RequestBody UserRequest request, Authentication authentication) {
        UserAccount currentUser = authService.current(authentication);
        return userService.create(request, currentUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request,
                               Authentication authentication) {
        UserAccount currentUser = authService.current(authentication);
        return userService.update(id, request, currentUser);
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        UserAccount currentUser = authService.current(authentication);
        return userService.updateProfile(currentUser.getId(), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    /** The Aadhaar card photo; staff only, and never cached since it is a government ID. */
    @GetMapping("/{id}/aadhaar")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<byte[]> aadhaar(@PathVariable Long id) {
        PhotoStorage.Photo photo = userService.aadhaarPhoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(photo.data());
    }

    @PutMapping(value = "/{id}/aadhaar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void saveAadhaar(@PathVariable Long id, @RequestPart("photo") MultipartFile photo,
                            Authentication authentication) {
        userService.saveAadhaarPhoto(id, photo, authService.current(authentication));
    }

    @DeleteMapping("/{id}/aadhaar")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void deleteAadhaar(@PathVariable Long id, Authentication authentication) {
        userService.deleteAadhaarPhoto(id, authService.current(authentication));
    }
}
