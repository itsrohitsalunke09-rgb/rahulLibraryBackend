package org.ngolibrary.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.repo.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Forgot password: a 6-digit code is emailed to the account's address and, with a new
 * password, resets it. Answers are the same whether or not an account uses the email,
 * so the form can't be used to find out who has an account.
 */
@Service
public class PasswordResetService {

    static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
    static final Duration RESEND_AFTER = Duration.ofSeconds(60);
    static final int MAX_ATTEMPTS = 5;
    static final String INVALID_CODE = "The code is wrong or has expired. Ask for a new code.";

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private final SecureRandom random = new SecureRandom();

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final EmailSender email;

    public PasswordResetService(UserAccountRepository users, PasswordEncoder encoder, EmailSender email) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
    }

    /** Emails a new code to the active account with this address, if there is exactly one. */
    @Transactional
    public void sendCode(String address) {
        UserAccount user = accountFor(required(address, "Enter your email address."));
        if (user == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = user.getResetCodeExpiresAt();
        if (expires != null && expires.minus(CODE_LIFETIME).plus(RESEND_AFTER).isAfter(now)) {
            return; // A code went out less than a minute ago.
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        user.setResetCodeHash(encoder.encode(code));
        user.setResetCodeExpiresAt(now.plus(CODE_LIFETIME));
        user.setResetCodeAttempts(0);
        users.save(user);
        // The code in the subject: each email starts its own Gmail conversation (not joining old ones
        // filed in Spam or archived), and the code shows in the phone's notification.
        email.send(user.getEmail(), user.getFullName(), "राहुल सार्वजनिक वाचनालय: पासवर्ड बदलण्याचा कोड " + code,
                "नमस्कार " + user.getFullName() + ",\n\n"
                        + "तुमचा पासवर्ड बदलण्याचा कोड आहे: " + code + "\n"
                        + "हा कोड पुढील १० मिनिटांसाठी वैध आहे.\n\n"
                        + "तुमचे युजरनेम: " + user.getUsername() + "\n\n"
                        + "तुम्ही पासवर्ड बदलण्याची विनंती केली नसेल, तर या ईमेलकडे दुर्लक्ष करा; तुमचा पासवर्ड बदलणार नाही.\n\n"
                        + "धन्यवाद,\n"
                        + "राहुल सार्वजनिक वाचनालय");
    }

    /** Sets a new password when the code is right, unused, not expired and not guessed too often. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void resetPassword(String address, String code, String newPassword) {
        String email = required(address, "Enter your email address.");
        String given = required(code, "Enter the code from the email.");
        if (newPassword == null || newPassword.length() < 6) {
            throw bad("Password must be at least 6 characters.");
        }
        UserAccount user = accountFor(email);
        if (user == null || user.getResetCodeHash() == null || user.getResetCodeExpiresAt() == null
                || user.getResetCodeExpiresAt().isBefore(LocalDateTime.now())
                || user.getResetCodeAttempts() >= MAX_ATTEMPTS) {
            throw bad(INVALID_CODE);
        }
        if (!encoder.matches(given, user.getResetCodeHash())) {
            user.setResetCodeAttempts(user.getResetCodeAttempts() + 1);
            users.save(user);
            throw bad(INVALID_CODE);
        }
        user.setPassword(encoder.encode(newPassword));
        user.setResetCodeHash(null);
        user.setResetCodeExpiresAt(null);
        user.setResetCodeAttempts(null);
        users.save(user);
    }

    private UserAccount accountFor(String email) {
        List<UserAccount> matches = users.findByEmailIgnoreCaseAndActiveTrue(email);
        if (matches.size() > 1) {
            // shortcut: email isn't unique in the database; with several accounts on one address, reset is refused.
            log.warn("Password reset refused: {} active accounts share one email address", matches.size());
        }
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw bad(message);
        }
        return value.trim();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
