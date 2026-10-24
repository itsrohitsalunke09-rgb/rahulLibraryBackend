package org.ngolibrary.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.ProfileUpdateRequest;
import org.ngolibrary.dto.UserRequest;
import org.ngolibrary.dto.UserResponse;
import org.ngolibrary.repo.BookIssueRepository;
import org.ngolibrary.repo.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final BookIssueRepository issues;

    public UserService(UserAccountRepository users, PasswordEncoder encoder, BookIssueRepository issues) {
        this.users = users;
        this.encoder = encoder;
        this.issues = issues;
    }

    public Page<UserResponse> listPaged(Role role, String q, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            List<Role> roles = role == null ? List.of(Role.values()) : List.of(role);
            return users.search(roles, q.trim(), pageable).map(UserResponse::from);
        }
        if (role == null) {
            return users.findAll(pageable).map(UserResponse::from);
        }
        return users.findByRole(role, pageable).map(UserResponse::from);
    }

    public List<UserResponse> searchStudents(String q) {
        return users.searchStudents(q).stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public UserResponse create(UserRequest request, UserAccount currentUser) {
        checkCanAssignRole(currentUser, request.getRole());
        if (users.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        UserAccount user = new UserAccount();
        apply(user, request, true);
        return UserResponse.from(users.save(user));
    }

    public UserResponse update(Long id, UserRequest request, UserAccount currentUser) {
        UserAccount user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (currentUser.getRole() != Role.ADMIN && user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an admin can edit admin accounts");
        }
        checkCanAssignRole(currentUser, request.getRole());
        if (!user.getUsername().equals(request.getUsername()) && users.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        apply(user, request, false);
        return UserResponse.from(users.save(user));
    }

    private void checkCanAssignRole(UserAccount currentUser, Role role) {
        if (currentUser.getRole() != Role.ADMIN && role == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an admin can assign the admin role");
        }
    }

    private void apply(UserAccount user, UserRequest request, boolean creating) {
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setActive(request.getActive() == null || request.getActive());
        if (creating || (request.getPassword() != null && !request.getPassword().isBlank())) {
            user.setPassword(encoder.encode(request.getPassword()));
        }
    }

    public UserResponse updateProfile(Long id, ProfileUpdateRequest request) {
        UserAccount user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            try {
                user.setDateOfBirth(LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth must be a date like 2010-05-31");
            }
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(encoder.encode(request.getPassword()));
        }
        return UserResponse.from(users.save(user));
    }

    public void delete(Long id) {
        if (!users.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        UserAccount user = users.findById(id).orElseThrow();
        if (user.getRole() == Role.STUDENT) {
            long activeIssues = issues.countByStudentIdAndStatus(id, org.ngolibrary.domain.IssueStatus.ISSUED);
            if (activeIssues > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot delete this student because they have " + activeIssues + " active issue(s). Please return all books first.");
            }
        }
        users.deleteById(id);
    }
}
