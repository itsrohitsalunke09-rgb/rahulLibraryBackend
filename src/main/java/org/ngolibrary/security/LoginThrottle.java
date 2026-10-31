package org.ngolibrary.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Slows password guessing on the sign-in page: after 5 wrong passwords for a username within
 * 15 minutes, that username must wait out the rest of the 15 minutes. A correct sign-in clears it.
 * Unknown usernames count too, so the lock doesn't reveal which accounts exist.
 */
@Component
public class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    // shortcut: in memory, so it resets on restart and isn't shared between servers; fine for one Render instance.
    private final Map<String, Failures> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public LoginThrottle() {
        this(Clock.systemUTC());
    }

    LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    private record Failures(Instant first, int count) {
        boolean over(Instant now) {
            return !first.plus(WINDOW).isAfter(now);
        }
    }

    /** Refuses (429) while this username has too many recent wrong passwords. */
    public void check(String username) {
        Failures f = failures.get(key(username));
        Instant now = clock.instant();
        if (f != null && f.count() >= MAX_FAILURES && !f.over(now)) {
            long minutes = Math.max(1, (Duration.between(now, f.first().plus(WINDOW)).toSeconds() + 59) / 60);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong attempts. Try again in " + minutes + " minute" + (minutes == 1 ? "" : "s") + ".");
        }
    }

    public void failed(String username) {
        Instant now = clock.instant();
        // Many different usernames tried: drop the old ones so memory stays small.
        if (failures.size() > 10_000) {
            failures.values().removeIf(f -> f.over(now));
        }
        failures.merge(key(username), new Failures(now, 1),
                (old, fresh) -> old.over(now) ? fresh : new Failures(old.first(), old.count() + 1));
    }

    public void succeeded(String username) {
        failures.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
