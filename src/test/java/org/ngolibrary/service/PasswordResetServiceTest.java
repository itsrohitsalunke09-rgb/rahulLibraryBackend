package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.repo.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class PasswordResetServiceTest {

    private UserAccountRepository users;
    private EmailSender email;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private PasswordResetService service;
    private UserAccount asha;

    @BeforeEach
    void setUp() {
        users = mock(UserAccountRepository.class);
        email = mock(EmailSender.class);
        service = new PasswordResetService(users, encoder, email);
        asha = new UserAccount();
        asha.setId(1L);
        asha.setUsername("asha");
        asha.setFullName("Asha Patil");
        asha.setEmail("asha@example.com");
        asha.setRole(Role.STUDENT);
        asha.setPassword(encoder.encode("old-pass"));
        when(users.findByEmailIgnoreCaseAndActiveTrue("asha@example.com")).thenReturn(List.of(asha));
    }

    /** Requests a code and returns the one that was emailed. */
    private String emailedCode() {
        service.sendCode(" asha@example.com ");
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(email).send(eq("asha@example.com"), eq("Asha Patil"), anyString(), text.capture());
        Matcher m = Pattern.compile("कोड आहे: (\\d{6})").matcher(text.getValue());
        assertTrue(m.find());
        return m.group(1);
    }

    private static void assertBadRequest(String message, Runnable call) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, call::run);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals(message, e.getReason());
    }

    @Test
    void theEmailedCodeResetsThePasswordOnce() {
        String code = emailedCode();
        assertTrue(!code.equals(asha.getResetCodeHash()), "only a hash is stored");

        service.resetPassword("asha@example.com", code, "new-pass");
        assertTrue(encoder.matches("new-pass", asha.getPassword()));
        assertNull(asha.getResetCodeHash());

        assertBadRequest(PasswordResetService.INVALID_CODE, () -> service.resetPassword("asha@example.com", code, "again1"));
    }

    @Test
    void unknownEmailSendsNothingAndDoesNotSayWhy() {
        when(users.findByEmailIgnoreCaseAndActiveTrue("nobody@example.com")).thenReturn(List.of());
        service.sendCode("nobody@example.com");
        verify(email, never()).send(any(), any(), any(), any());
        assertBadRequest(PasswordResetService.INVALID_CODE, () -> service.resetPassword("nobody@example.com", "123456", "new-pass"));
    }

    @Test
    void wrongCodesLockTheCodeAfterFiveTries() {
        String code = emailedCode();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < PasswordResetService.MAX_ATTEMPTS; i++) {
            assertBadRequest(PasswordResetService.INVALID_CODE, () -> service.resetPassword("asha@example.com", wrong, "new-pass"));
        }
        assertBadRequest(PasswordResetService.INVALID_CODE, () -> service.resetPassword("asha@example.com", code, "new-pass"));
        assertTrue(encoder.matches("old-pass", asha.getPassword()));
    }

    @Test
    void anExpiredCodeDoesNotWork() {
        String code = emailedCode();
        asha.setResetCodeExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertBadRequest(PasswordResetService.INVALID_CODE, () -> service.resetPassword("asha@example.com", code, "new-pass"));
    }

    @Test
    void aSecondRequestWithinAMinuteSendsNoNewEmail() {
        emailedCode();
        service.sendCode("asha@example.com");
        verify(email).send(any(), any(), any(), any());
    }

    @Test
    void shortPasswordsAreRejected() {
        String code = emailedCode();
        assertBadRequest("Password must be at least 6 characters.", () -> service.resetPassword("asha@example.com", code, "12345"));
    }
}
