package org.ngolibrary.dto;

/** Forgot password: the email alone asks for a code; with the code and a new password it resets. */
public record PasswordResetRequest(String email, String code, String password) {
}
