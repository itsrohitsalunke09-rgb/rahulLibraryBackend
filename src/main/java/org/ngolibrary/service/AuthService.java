package org.ngolibrary.service;

import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.AuthResponse;
import org.ngolibrary.dto.LoginRequest;
import org.ngolibrary.dto.RegistrationRequest;
import org.ngolibrary.repo.UserAccountRepository;
import org.ngolibrary.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!user.isActive() || !encoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return new AuthResponse(jwtService.generate(user), user.getId(), user.getUsername(),
                user.getFullName(), user.getRole());
    }

    public UserAccount current(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public AuthResponse register(RegistrationRequest request) {
        if (users.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(Role.STUDENT);
        user.setActive(true);
        users.save(user);
        return new AuthResponse(jwtService.generate(user), user.getId(), user.getUsername(),
                user.getFullName(), user.getRole());
    }
}
