package org.ngolibrary.web;

import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.ApiMessage;
import org.ngolibrary.dto.AuthResponse;
import org.ngolibrary.dto.LoginRequest;
import org.ngolibrary.dto.RegistrationRequest;
import org.ngolibrary.dto.UserResponse;
import org.ngolibrary.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegistrationRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        UserAccount user = authService.current(authentication.getName());
        return UserResponse.from(user);
    }

    @GetMapping("/health")
    public ApiMessage health() {
        return new ApiMessage("ok");
    }
}
