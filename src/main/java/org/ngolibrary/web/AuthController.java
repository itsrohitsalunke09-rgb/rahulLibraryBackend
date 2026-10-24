package org.ngolibrary.web;

import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.dto.ApiMessage;
import org.ngolibrary.dto.AuthResponse;
import org.ngolibrary.dto.LoginRequest;
import org.ngolibrary.dto.PasswordResetRequest;
import org.ngolibrary.dto.RegistrationRequest;
import org.ngolibrary.dto.UserResponse;
import org.ngolibrary.service.AuthService;
import org.ngolibrary.service.PasswordResetService;
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
    private final PasswordResetService passwordReset;

    public AuthController(AuthService authService, PasswordResetService passwordReset) {
        this.authService = authService;
        this.passwordReset = passwordReset;
    }

    /** Always the same answer, so it doesn't reveal whether an account uses the email. */
    @PostMapping("/forgot-password")
    public ApiMessage forgotPassword(@RequestBody PasswordResetRequest request) {
        passwordReset.sendCode(request.email());
        return new ApiMessage("If an account uses this email, a code is on its way.");
    }

    @PostMapping("/reset-password")
    public ApiMessage resetPassword(@RequestBody PasswordResetRequest request) {
        passwordReset.resetPassword(request.email(), request.code(), request.password());
        return new ApiMessage("Your password has been changed.");
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
        UserAccount user = authService.current(authentication);
        return UserResponse.from(user);
    }

    @GetMapping("/health")
    public ApiMessage health() {
        return new ApiMessage("ok");
    }
}
