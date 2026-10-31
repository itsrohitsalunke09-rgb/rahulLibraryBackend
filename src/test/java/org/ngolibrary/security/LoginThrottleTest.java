package org.ngolibrary.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LoginThrottleTest {

    /** A clock the test moves forward. */
    private static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-10-31T10:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final TestClock clock = new TestClock();
    private final LoginThrottle throttle = new LoginThrottle(clock);

    private void fail(String username, int times) {
        for (int i = 0; i < times; i++) {
            throttle.check(username);
            throttle.failed(username);
        }
    }

    @Test
    void fiveWrongPasswordsLockTheUsernameForTheRestOfFifteenMinutes() {
        fail("asha", 4);
        assertDoesNotThrow(() -> throttle.check("asha"));
        throttle.failed("asha");
        ResponseStatusException locked = assertThrows(ResponseStatusException.class, () -> throttle.check("asha"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, locked.getStatus());
        assertTrue(locked.getReason().contains("15 minutes"), locked.getReason());
        // Another username is not affected; case and spaces don't get around it.
        assertDoesNotThrow(() -> throttle.check("ravi"));
        assertThrows(ResponseStatusException.class, () -> throttle.check(" ASHA "));

        clock.now = clock.now.plus(Duration.ofMinutes(14));
        assertTrue(assertThrows(ResponseStatusException.class, () -> throttle.check("asha")).getReason().contains("1 minute."));
        clock.now = clock.now.plus(Duration.ofMinutes(1));
        assertDoesNotThrow(() -> throttle.check("asha"));
    }

    @Test
    void failuresOlderThanTheWindowStartOver() {
        fail("asha", 4);
        clock.now = clock.now.plus(Duration.ofMinutes(16));
        fail("asha", 4);
        assertDoesNotThrow(() -> throttle.check("asha"));
    }

    @Test
    void aCorrectSignInClearsTheCount() {
        fail("asha", 4);
        throttle.succeeded("asha");
        fail("asha", 4);
        assertDoesNotThrow(() -> throttle.check("asha"));
    }
}
