package org.ngolibrary.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    public List<UserResponse> list(Role role) {
        List<UserAccount> accounts = role == null ? users.findAll() : users.findByRole(role);
        return accounts.stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public Page<UserResponse> listPaged(Role role, Pageable pageable) {
        if (role == null) {
            return users.findAll(pageable).map(UserResponse::from);
        }
        return users.findByRole(role, pageable).map(UserResponse::from);
    }

    public List<UserResponse> searchStudents(String q) {
        return users.searchStudents(q).stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public UserResponse create(UserRequest request) {
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

    public UserResponse update(Long id, UserRequest request) {
        UserAccount user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.getUsername().equals(request.getUsername()) && users.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        apply(user, request, false);
        return UserResponse.from(users.save(user));
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
            } catch (Exception ignored) {
                // ignore invalid date format
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
